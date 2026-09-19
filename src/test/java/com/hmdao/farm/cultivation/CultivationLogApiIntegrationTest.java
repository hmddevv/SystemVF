package com.hmdao.farm.cultivation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Kịch bản thật của một vườn cà phê Robusta: chi phí chăm sóc rải từ tháng 3, thu hoạch tháng 11
 * và tháng 1 năm sau. Phép thử quan trọng nhất là cả ba mốc đó phải rơi vào <b>cùng một niên vụ</b>
 * (BR-05a) — gom theo năm dương lịch sẽ tách doanh thu tháng 1 khỏi chi phí đã bỏ ra.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CultivationLogApiIntegrationTest {

    /** Cà phê Robusta trong V2: niên vụ bắt đầu tháng 2. */
    private static final long COFFEE = 1L;

    @Autowired
    MockMvc mvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void oneProductionCycleGathersItsCostsAndRevenueEvenAcrossNewYear() throws Exception {
        long plantingId = plantCoffee();

        logActivity(plantingId, "FERTILIZING", "2025-03-15", "10000000");
        logActivity(plantingId, "SPRAYING", "2025-06-10", "2500000");
        harvest(plantingId, "2025-11-28", 3200, "76800000");
        harvest(plantingId, "2026-01-15", 800, "20000000");
        // Sang tháng 2 là chu kỳ sản xuất mới -> niên vụ khác
        logActivity(plantingId, "WATERING", "2026-03-01", "1800000");

        // BR-05: ba bản ghi của vụ 2025 dùng chung một niên vụ, không sinh mỗi bản ghi một vụ
        mvc.perform(get("/api/v1/plantings/{id}/seasons", plantingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].label").value("2026/2027"))
                .andExpect(jsonPath("$[1].label").value("2025/2026"))
                .andExpect(jsonPath("$[1].startDate").value("2025-02-01"))
                .andExpect(jsonPath("$[1].endDate").value("2026-01-31"))
                .andExpect(jsonPath("$[1].cropName").value("Cà phê (Robusta)"))
                .andExpect(jsonPath("$[1].activityCount").value(2))
                .andExpect(jsonPath("$[1].harvestCount").value(2))
                .andExpect(jsonPath("$[1].totalQuantityKg").value(4000.0))
                .andExpect(jsonPath("$[1].totalCost").value(12500000.00))
                .andExpect(jsonPath("$[1].totalRevenue").value(96800000.00))
                .andExpect(jsonPath("$[1].netProfit").value(84300000.00));
    }

    @Test
    void firstHarvestPromotesAGrowingPlantingAndBlocksDeletionOfItsHistory() throws Exception {
        long plantingId = plantCoffee(false);

        mvc.perform(get("/api/v1/plantings/{id}", plantingId))
                .andExpect(jsonPath("$.status").value("GROWING"));

        harvest(plantingId, "2025-11-28", 1500, "36000000");

        // BR-09: có thu hoạch nghĩa là vườn đã vào giai đoạn kinh doanh
        mvc.perform(get("/api/v1/plantings/{id}", plantingId))
                .andExpect(jsonPath("$.status").value("PRODUCING"));

        // BR-10: lứa trồng đã có niên vụ thì không xóa được, muốn bỏ thật phải dùng /termination
        mvc.perform(delete("/api/v1/plantings/{id}", plantingId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR-10"));
    }

    @Test
    void rejectsEntriesOutsideThePlantingLifetime() throws Exception {
        long plantingId = plantCoffee();

        // BR-07: trước ngày trồng
        mvc.perform(activityRequest(plantingId, "WATERING", "2015-01-01", "0"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-07"));

        // BR-07: ở tương lai
        mvc.perform(activityRequest(plantingId, "WATERING", LocalDate.now().plusDays(1).toString(), "0"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-07"));

        // BR-12: loại OTHER không có ghi chú thì dòng nhật ký vô nghĩa
        mvc.perform(activityRequest(plantingId, "OTHER", "2025-06-10", "500000"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-12"));
    }

    @Test
    void seasonIsDeletableOnlyAfterItsEntriesAreGone() throws Exception {
        long plantingId = plantCoffee();
        long activityId = logActivity(plantingId, "WEEDING", "2025-05-05", "900000");
        long seasonId = firstSeasonId(plantingId);

        mvc.perform(delete("/api/v1/seasons/{id}", seasonId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR-10"));

        mvc.perform(delete("/api/v1/activities/{id}", activityId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/seasons/{id}", seasonId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/plantings/{id}/seasons", plantingId))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void correctingTheDateMovesTheEntryToTheMatchingSeason() throws Exception {
        long plantingId = plantCoffee();
        long activityId = logActivity(plantingId, "PRUNING", "2025-06-10", "700000");

        mvc.perform(get("/api/v1/activities/{id}", activityId))
                .andExpect(jsonPath("$.seasonLabel").value("2025/2026"));

        mvc.perform(put("/api/v1/activities/{id}", activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "PRUNING", "activityDate": "2026-04-02", "cost": 700000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonLabel").value("2026/2027"));

        // Niên vụ cũ rỗng đi, niên vụ mới nhận bản ghi
        mvc.perform(get("/api/v1/plantings/{id}/seasons", plantingId))
                .andExpect(jsonPath("$[0].label").value("2026/2027"))
                .andExpect(jsonPath("$[0].activityCount").value(1))
                .andExpect(jsonPath("$[1].label").value("2025/2026"))
                .andExpect(jsonPath("$[1].activityCount").value(0));
    }

    @Test
    void activityLogIsPaged() throws Exception {
        long plantingId = plantCoffee();
        for (int day = 1; day <= 5; day++) {
            logActivity(plantingId, "WATERING", "2025-06-0%d".formatted(day), "100000");
        }
        long seasonId = firstSeasonId(plantingId);

        mvc.perform(get("/api/v1/seasons/{id}/activities", seasonId).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                // mới nhất trước
                .andExpect(jsonPath("$.content[0].activityDate").value("2025-06-05"));

        mvc.perform(get("/api/v1/seasons/{id}/activities", seasonId).param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].activityDate").value("2025-06-01"));
    }

    @Test
    void seasonSummaryDoesNotTriggerNPlusOneQueries() throws Exception {
        long plantingId = plantCoffee();
        for (int year = 2021; year <= 2025; year++) {
            logActivity(plantingId, "FERTILIZING", "%d-06-10".formatted(year), "1000000");
            harvest(plantingId, "%d-11-20".formatted(year), 1000, "24000000");
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mvc.perform(get("/api/v1/plantings/{id}/seasons", plantingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        // 1 kiểm tra user + 1 kiểm tra quyền sở hữu lứa trồng + 1 lấy niên vụ kèm cây trồng
        // + 1 GROUP BY chi phí + 1 GROUP BY thu hoạch. Cộng dồn từng niên vụ sẽ là 3 + 2N.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
    }

    @Test
    void br11_dataOfAnotherOwnerIsNotFound() throws Exception {
        long plantingId = plantCoffee();
        logActivity(plantingId, "WATERING", "2025-06-10", "100000");
        long seasonId = firstSeasonId(plantingId);

        mvc.perform(get("/api/v1/seasons/{id}", seasonId).header("X-User-Id", 2))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.rule").value("BR-11"));
        mvc.perform(get("/api/v1/plantings/{id}/seasons", plantingId).header("X-User-Id", 2))
                .andExpect(status().isNotFound());
    }

    private long plantCoffee() throws Exception {
        return plantCoffee(true);
    }

    private long plantCoffee(boolean alreadyProducing) throws Exception {
        String farm = mvc.perform(post("/api/v1/farms").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nông trại cà phê"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long farmId = JsonPath.<Number>read(farm, "$.id").longValue();
        String plot = mvc.perform(post("/api/v1/farms/{id}/plots", farmId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lô A2", "areaM2": 15000}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long plotId = JsonPath.<Number>read(plot, "$.id").longValue();
        String planting = mvc.perform(post("/api/v1/plots/{id}/plantings", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cropId": %d, "plantingDate": "2016-06-15", "treeCount": 1100, \
                                "alreadyProducing": %s}
                                """.formatted(COFFEE, alreadyProducing)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(planting, "$.id").longValue();
    }

    private MockHttpServletRequestBuilder activityRequest(long plantingId, String type, String date, String cost) {
        return post("/api/v1/plantings/{id}/activities", plantingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"type": "%s", "activityDate": "%s", "cost": %s}
                        """.formatted(type, date, cost));
    }

    private long logActivity(long plantingId, String type, String date, String cost) throws Exception {
        String body = mvc.perform(activityRequest(plantingId, type, date, cost))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private long harvest(long plantingId, String date, double quantityKg, String revenue) throws Exception {
        String body = mvc.perform(post("/api/v1/plantings/{id}/harvests", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"harvestDate": "%s", "quantityKg": %s, "revenue": %s}
                                """.formatted(date, quantityKg, revenue)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private long firstSeasonId(long plantingId) throws Exception {
        String body = mvc.perform(get("/api/v1/plantings/{id}/seasons", plantingId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$[0].id").longValue();
    }
}
