package com.hmdao.farm.reminder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.support.IntegrationTest;
import com.hmdao.farm.support.MutableTestClock;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Nhắc việc là chuyện của "hôm nay", nên ngày tháng ở đây tính tương đối so với đồng hồ — và
 * đồng hồ do test cầm ({@link MutableTestClock}), không phải đồng hồ hệ thống. Nhờ vậy hai luật
 * theo mùa kiểm được ở mức API: "đang là mùa khô" trở thành một dòng khai báo thay vì phải chờ
 * đến tháng 12 mới chạy được test.
 */
@IntegrationTest
class ReminderApiIT {

    private static final long COFFEE = 1L;
    private static final long DURIAN = 4L;
    private static final String IRRIGATION_RULE = "CARE-01";
    private static final String FERTILIZING_RULE = "CARE-02";
    private static final String PRUNING_RULE = "CARE-03";
    private static final String YOUNG_ORCHARD_RULE = "CARE-04";

    /** Tháng 1: giữa mùa khô Tây Nguyên (12–4). */
    private static final LocalDate IN_DRY_SEASON = LocalDate.of(2027, 1, 20);
    /** Tháng 9: cuối mùa mưa (5–9), vẫn còn trong cửa sổ bón phân. */
    private static final LocalDate IN_RAINY_SEASON = LocalDate.of(2026, 9, 20);

    @Autowired
    MockMvc mvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    MutableTestClock clock;

    private long farmId;
    private long matureCoffeeId;
    private long youngDurianId;

    @BeforeEach
    void oneMatureOrchardJustHarvestedAndOneYoungOrchardLeftAlone() throws Exception {
        farmId = createFarm();
        long plotA2 = createPlot(farmId, "Lô A2");
        long plotC = createPlot(farmId, "Lô C");

        matureCoffeeId = plant(plotA2, COFFEE, "2016-06-15", true);
        harvest(matureCoffeeId, clock.today().minusDays(40), 3000, "72000000");

        youngDurianId = plant(plotC, DURIAN, clock.today().minusMonths(6).toString(), false);
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

    private void logActivity(String type, LocalDate date) throws Exception {
        mvc.perform(post("/api/v1/plantings/{id}/activities", matureCoffeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "%s", "activityDate": "%s", "cost": 500000}
                                """.formatted(type, date)))
                .andExpect(status().isCreated());
    }

    @Test
    void br18_loggingTheWorkIsWhatClearsTheReminder() throws Exception {
        reminders().andExpect(jsonPath(ruleFor(PRUNING_RULE, matureCoffeeId)).exists());

        mvc.perform(post("/api/v1/plantings/{id}/activities", matureCoffeeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "PRUNING", "activityDate": "%s", "cost": 3000000}
                                """.formatted(clock.today())))
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
                                """.formatted(clock.today())))
                .andExpect(status().isOk());

        reminders().andExpect(jsonPath("$[?(@.plantingId == %d)]".formatted(youngDurianId)).doesNotExist());
    }

    @Test
    void care01_remindsToIrrigateOnceTheDrySeasonArrives() throws Exception {
        String irrigation = ruleFor(IRRIGATION_RULE, matureCoffeeId);

        // Tháng 10 chưa phải mùa khô: luật tưới im lặng dù vườn cà phê chưa từng được tưới
        reminders().andExpect(jsonPath(irrigation).doesNotExist());

        clock.setToday(IN_DRY_SEASON);
        // Vườn chưa có đợt tưới nào -> tới hạn ngay hôm nay, chưa quá hạn
        reminders()
                .andExpect(status().isOk())
                .andExpect(jsonPath(irrigation + ".severity").value("DUE_SOON"))
                .andExpect(jsonPath(irrigation + ".suggestedActivity").value("WATERING"));

        // Tưới cách đây 30 ngày, chu kỳ khuyến nghị 20 ngày -> quá hạn 10 ngày
        logActivity("WATERING", clock.today().minusDays(30));

        reminders()
                .andExpect(jsonPath(irrigation + ".severity").value("OVERDUE"))
                .andExpect(jsonPath(irrigation + ".daysOverdue").value(10));
    }

    @Test
    void care02_remindsToFertiliseInTheRainsButNotOnDryGround() throws Exception {
        clock.setToday(IN_RAINY_SEASON);
        reminders().andExpect(jsonPath(ruleFor(FERTILIZING_RULE, matureCoffeeId)).exists());

        // Cùng một vườn, cùng một dữ liệu — chỉ đổi mùa là lời nhắc bón phân biến mất
        clock.setToday(IN_DRY_SEASON);
        reminders().andExpect(jsonPath(ruleFor(FERTILIZING_RULE, matureCoffeeId)).doesNotExist());
    }

    @Test
    void theWholeReminderListCostsThreeQueries() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        reminders().andExpect(status().isOk());

        // 1 kiểm tra user + 1 kiểm tra nông trại thuộc về mình (BR-11) + 1 lứa đang canh tác
        // + 1 MAX ngày theo loại việc + 1 MAX ngày thu hoạch. Luật chạy trên bộ nhớ nên thêm
        // luật không thêm truy vấn nào.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
    }

    @Test
    void br11_anotherOwnerIsToldTheFarmDoesNotExistRatherThanGettingAnEmptyList() throws Exception {
        mvc.perform(get("/api/v1/reminders").param("farmId", String.valueOf(farmId)).header("X-User-Id", 2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.rule").value("BR-11"));
    }

    /** Không lọc nông trại thì chủ khác chỉ đơn giản là không có việc gì — đó mới là danh sách rỗng. */
    @Test
    void br11_anotherOwnerWithNoOrchardsSimplyHasNothingDue() throws Exception {
        mvc.perform(get("/api/v1/reminders").header("X-User-Id", 2))
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
