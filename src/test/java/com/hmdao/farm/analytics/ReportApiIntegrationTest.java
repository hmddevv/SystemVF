package com.hmdao.farm.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hamcrest.Matchers;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Lô A2 trồng xen cà phê và hồ tiêu, lô B1 mới trồng sầu riêng chưa ghi gì.
 *
 * <p>Hai phép thử đáng giá nhất ở đây: doanh thu hồ tiêu tháng 3/2026 phải rơi vào **niên vụ
 * 2025** cùng với chi phí tháng 6/2025 (BR-13, nhờ BR-05a), và cà phê lỗ vụ 2024 rồi lãi vụ
 * 2025 phải cho ra niên vụ hoàn vốn chứ không phải hai kết luận trái ngược (BR-17).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReportApiIntegrationTest {

    private static final long COFFEE = 1L;
    private static final long PEPPER = 3L;
    private static final long DURIAN = 4L;

    @Autowired
    MockMvc mvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private long farmId;
    private long coffeeId;

    @BeforeEach
    void intercroppedFarmWithTwoSeasonsOfHistory() throws Exception {
        farmId = createFarm();
        long plotA2 = createPlot(farmId, "Lô A2", 15_000);
        long plotB1 = createPlot(farmId, "Lô B1", 5_000);

        coffeeId = plant(plotA2, COFFEE, "2016-06-15", 1100, true);
        long pepperId = plant(plotA2, PEPPER, "2019-07-01", 400, true);
        plant(plotB1, DURIAN, "2023-05-20", 80, false);

        // Cà phê: niên vụ bắt đầu tháng 2 -> vụ 2024 lỗ, vụ 2025 lãi
        activity(coffeeId, "FERTILIZING", "2024-03-15", "30000000");
        harvest(coffeeId, "2024-11-20", 1000, "24000000");
        activity(coffeeId, "FERTILIZING", "2025-03-15", "40000000");
        harvest(coffeeId, "2025-11-28", 3000, "72000000");

        // Hồ tiêu: niên vụ bắt đầu tháng 5 -> vụ 2025 chạy tới 30/4/2026
        activity(pepperId, "SPRAYING", "2025-06-10", "10000000");
        harvest(pepperId, "2026-03-15", 500, "60000000");
    }

    @Test
    void groupsByCropAcrossEverySeasonAndRanksByProfit() throws Exception {
        report("CROP", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(3))
                .andExpect(jsonPath("$.rows[0].label").value("Hồ tiêu (Vĩnh Linh)"))
                .andExpect(jsonPath("$.rows[0].netProfit").value(50_000_000.0))
                .andExpect(jsonPath("$.rows[1].label").value("Cà phê (Robusta)"))
                .andExpect(jsonPath("$.rows[1].totalCost").value(70_000_000.0))
                .andExpect(jsonPath("$.rows[1].totalRevenue").value(96_000_000.0))
                .andExpect(jsonPath("$.rows[1].netProfit").value(26_000_000.0))
                .andExpect(jsonPath("$.rows[1].seasonCount").value(2))
                .andExpect(jsonPath("$.total.netProfit").value(76_000_000.0))
                // Lô A2 trồng xen nên diện tích chỉ được tính một lần trong dòng tổng
                .andExpect(jsonPath("$.total.areaM2").value(20_000.0))
                .andExpect(jsonPath("$.total.treeCount").value(1580));
    }

    @Test
    void br13_aSeasonKeepsItsCostsAndRevenueTogetherEvenAcrossCalendarYears() throws Exception {
        // Chi phí phun thuốc 6/2025 và doanh thu 3/2026 cùng thuộc niên vụ hồ tiêu 2025
        report("CROP", 2025)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].label").value("Hồ tiêu (Vĩnh Linh)"))
                .andExpect(jsonPath("$.rows[0].totalCost").value(10_000_000.0))
                .andExpect(jsonPath("$.rows[0].totalRevenue").value(60_000_000.0))
                .andExpect(jsonPath("$.rows[0].seasonCount").value(1));
    }

    @Test
    void br14_theNewOrchardWithNoEntriesIsStillOnTheReport() throws Exception {
        report("CROP", null)
                .andExpect(jsonPath("$.rows[2].label").value("Sầu riêng (Ri6)"))
                .andExpect(jsonPath("$.rows[2].netProfit").value(0))
                .andExpect(jsonPath("$.rows[2].seasonCount").value(0))
                .andExpect(jsonPath("$.rows[2].treeCount").value(80));
    }

    @Test
    void br15_br16_normalisedFiguresAndTheIntercroppingFlag() throws Exception {
        report("PLOT", null)
                .andExpect(jsonPath("$.rows[0].label").value("Lô A2"))
                .andExpect(jsonPath("$.rows[0].netProfit").value(76_000_000.0))
                .andExpect(jsonPath("$.rows[0].areaM2").value(15_000.0))
                // 76.000.000 chia cho 15 nghìn m² -> lãi trên mỗi 1.000 m²
                .andExpect(jsonPath("$.rows[0].profitPer1000m2").value(5_066_666.67))
                .andExpect(jsonPath("$.rows[0].sharedPlot").value(true))
                .andExpect(jsonPath("$.rows[1].label").value("Lô B1"))
                .andExpect(jsonPath("$.rows[1].sharedPlot").value(false));
    }

    @Test
    void br17_paybackSeasonSurvivesTheYearFilter() throws Exception {
        String coffee = "$.rows[?(@.id == %d)]".formatted(coffeeId);
        report("PLANTING", 2025)
                .andExpect(status().isOk())
                // Nhìn riêng vụ 2025 thì lãi 32 triệu…
                .andExpect(jsonPath(coffee + ".netProfit").value(32_000_000.0))
                // …còn tính cả vụ 2024 lỗ 6 triệu thì luỹ kế là 26 triệu, hoà vốn từ vụ 2025
                .andExpect(jsonPath(coffee + ".lifetimeNetProfit").value(26_000_000.0))
                .andExpect(jsonPath(coffee + ".paybackYear").value(2025));
    }

    @Test
    void adr11_reportTotalsAgreeWithTheSeasonViewOfTheSamePlanting() throws Exception {
        String seasons = mvc.perform(get("/api/v1/plantings/{id}/seasons", coffeeId))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Báo cáo và màn hình niên vụ là hai đường đọc khác nhau — chúng phải cho cùng một con số
        assertThat(JsonPath.<Number>read(seasons, "$[0].year").intValue()).isEqualTo(2025);
        assertThat(JsonPath.<Number>read(seasons, "$[0].netProfit").longValue()).isEqualTo(32_000_000L);
        report("PLANTING", 2025)
                .andExpect(jsonPath("$.rows[?(@.id == %d)].netProfit".formatted(coffeeId))
                        .value(32_000_000.0));
    }

    @Test
    void wholeFarmReportCostsThreeQueriesNoMatterHowMuchHistoryThereIs() throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        report("CROP", null).andExpect(status().isOk());

        // 1 kiểm tra user + 1 hồ sơ lứa trồng + 1 GROUP BY chi phí + 1 GROUP BY thu hoạch
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @Test
    void farmFilterNarrowsTheScopeAndAnUnknownFarmYieldsNothing() throws Exception {
        mvc.perform(get("/api/v1/reports/profit-loss").param("farmId", String.valueOf(farmId)))
                .andExpect(jsonPath("$.rows.length()").value(3));
        // Không lọc thì thấy mọi nông trại của mình — nhánh "farmId is null" của truy vấn
        mvc.perform(get("/api/v1/reports/profit-loss"))
                .andExpect(jsonPath("$.rows.length()").value(Matchers.greaterThanOrEqualTo(3)));
        mvc.perform(get("/api/v1/reports/profit-loss").param("farmId", "999999"))
                .andExpect(jsonPath("$.rows.length()").value(0))
                .andExpect(jsonPath("$.total").doesNotExist());
    }

    @Test
    void br11_anotherOwnerSeesNothingOfThisFarm() throws Exception {
        mvc.perform(get("/api/v1/reports/profit-loss").header("X-User-Id", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(0));
    }

    /** Các test dùng chung một database, nên báo cáo luôn khoanh theo nông trại của chính test này. */
    private ResultActions report(String groupBy, Integer year) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/reports/profit-loss")
                .param("groupBy", groupBy)
                .param("farmId", String.valueOf(farmId));
        if (year != null) {
            request = request.param("year", String.valueOf(year));
        }
        return mvc.perform(request);
    }

    private long createFarm() throws Exception {
        String body = mvc.perform(post("/api/v1/farms").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nông trại phân tích"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private long createPlot(long farm, String name, int areaM2) throws Exception {
        String body = mvc.perform(post("/api/v1/farms/{id}/plots", farm).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "areaM2": %d}
                                """.formatted(name, areaM2)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private long plant(long plotId, long cropId, String date, int trees, boolean producing) throws Exception {
        String body = mvc.perform(post("/api/v1/plots/{id}/plantings", plotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cropId": %d, "plantingDate": "%s", "treeCount": %d, "alreadyProducing": %s}
                                """.formatted(cropId, date, trees, producing)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }

    private void activity(long plantingId, String type, String date, String cost) throws Exception {
        mvc.perform(post("/api/v1/plantings/{id}/activities", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "%s", "activityDate": "%s", "cost": %s}
                                """.formatted(type, date, cost)))
                .andExpect(status().isCreated());
    }

    private void harvest(long plantingId, String date, double quantityKg, String revenue) throws Exception {
        mvc.perform(post("/api/v1/plantings/{id}/harvests", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"harvestDate": "%s", "quantityKg": %s, "revenue": %s}
                                """.formatted(date, quantityKg, revenue)))
                .andExpect(status().isCreated());
    }
}
