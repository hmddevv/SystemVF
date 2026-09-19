package com.hmdao.farm.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Ngày tháng ở đây tính tương đối so với hôm nay, vì nhắc việc là chuyện của hiện tại. Các luật
 * phụ thuộc mùa (tưới mùa khô, bón mùa mưa) chỉ chạy vào đúng mùa nên test lọc theo mã luật thay
 * vì so cả danh sách — nếu không, test sẽ đỏ hay xanh tùy tháng chạy.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReminderApiIntegrationTest {

    private static final long COFFEE = 1L;
    private static final long DURIAN = 4L;
    private static final String PRUNING_RULE = "CARE-03";
    private static final String YOUNG_ORCHARD_RULE = "CARE-04";

    @Autowired
    MockMvc mvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private long farmId;
    private long matureCoffeeId;
    private long youngDurianId;

    @BeforeEach
    void oneMatureOrchardJustHarvestedAndOneYoungOrchardLeftAlone() throws Exception {
        farmId = createFarm();
        long plotA2 = createPlot(farmId, "Lô A2");
        long plotC = createPlot(farmId, "Lô C");

        matureCoffeeId = plant(plotA2, COFFEE, "2016-06-15", true);
        harvest(matureCoffeeId, LocalDate.now().minusDays(40), 3000, "72000000");

        youngDurianId = plant(plotC, DURIAN, LocalDate.now().minusMonths(6).toString(), false);
    }

    @Test
    void remindsToPruneAfterTheHarvestGracePeriod() throws Exception {
        String pruning = ruleFor(PRUNING_RULE, matureCoffeeId);

        reminders()
                .andExpect(status().isOk())
                .andExpect(jsonPath(pruning + ".severity").value("OVERDUE"))
                // Thu hoạch cách đây 40 ngày, hạn tỉa là 21 ngày sau đó
                .andExpect(jsonPath(pruning + ".daysOverdue").value(19))
                .andExpect(jsonPath(pruning + ".suggestedActivity").value("PRUNING"))
                .andExpect(jsonPath(pruning + ".cropName").value("Cà phê (Robusta)"))
                .andExpect(jsonPath(pruning + ".plotName").value("Lô A2"));
    }

    @Test
    void br18_loggingTheWorkIsWhatClearsTheReminder() throws Exception {
        reminders().andExpect(jsonPath(ruleFor(PRUNING_RULE, matureCoffeeId)).exists());

        mvc.perform(post("/api/v1/plantings/{id}/activities", matureCoffeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "PRUNING", "activityDate": "%s", "cost": 3000000}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated());

        // Không có nút "đã làm" nào — chính dòng nhật ký vừa ghi làm lời nhắc biến mất
        reminders().andExpect(jsonPath(ruleFor(PRUNING_RULE, matureCoffeeId)).doesNotExist());
    }

    @Test
    void remindsToCheckOnAYoungOrchardThatHasNoRecordsAtAll() throws Exception {
        String check = ruleFor(YOUNG_ORCHARD_RULE, youngDurianId);

        reminders()
                .andExpect(jsonPath(check + ".severity").value("OVERDUE"))
                .andExpect(jsonPath(check + ".cropName").value("Sầu riêng (Ri6)"))
                // Lọc JsonPath trả về mảng, nên matcher phải khớp phần tử chứ không khớp cả danh sách
                .andExpect(jsonPath(check + ".detail").value(
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("6 tháng tuổi"))));
    }

    @Test
    void br18_aTerminatedPlantingDropsOffTheListEntirely() throws Exception {
        reminders().andExpect(jsonPath(ruleFor(YOUNG_ORCHARD_RULE, youngDurianId)).exists());

        mvc.perform(post("/api/v1/plantings/{id}/termination", youngDurianId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "%s", "reason": "PEST_DISEASE", "note": "Chết do nấm"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isOk());

        reminders().andExpect(jsonPath("$[?(@.plantingId == %d)]".formatted(youngDurianId)).doesNotExist());
    }

    @Test
    void theWholeReminderListCostsThreeQueries() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        reminders().andExpect(status().isOk());

        // 1 kiểm tra user + 1 lứa đang canh tác + 1 MAX ngày theo loại việc + 1 MAX ngày thu hoạch.
        // Luật chạy trên bộ nhớ nên thêm luật không thêm truy vấn.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @Test
    void br11_anotherOwnerGetsNoRemindersFromThisFarm() throws Exception {
        mvc.perform(get("/api/v1/reminders").param("farmId", String.valueOf(farmId)).header("X-User-Id", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private ResultActions reminders() throws Exception {
        return mvc.perform(get("/api/v1/reminders").param("farmId", String.valueOf(farmId)));
    }

    private static String ruleFor(String ruleCode, long plantingId) {
        return "$[?(@.ruleCode == %s && @.plantingId == %d)]"
                .formatted("'" + ruleCode + "'", plantingId);
    }

    private long createFarm() throws Exception {
        String body = mvc.perform(post("/api/v1/farms").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nông trại nhắc việc"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private long createPlot(long farm, String name) throws Exception {
        String body = mvc.perform(post("/api/v1/farms/{id}/plots", farm).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "areaM2": 10000}
                                """.formatted(name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private long plant(long plotId, long cropId, String date, boolean producing) throws Exception {
        String body = mvc.perform(post("/api/v1/plots/{id}/plantings", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cropId": %d, "plantingDate": "%s", "treeCount": 500, "alreadyProducing": %s}
                                """.formatted(cropId, date, producing)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private void harvest(long plantingId, LocalDate date, double quantityKg, String revenue) throws Exception {
        mvc.perform(post("/api/v1/plantings/{id}/harvests", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"harvestDate": "%s", "quantityKg": %s, "revenue": %s}
                                """.formatted(date, quantityKg, revenue)))
                .andExpect(status().isCreated());
    }
}
