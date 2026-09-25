package com.hmdao.farm.cultivation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kịch bản thực tế: lô cà phê trồng xen hồ tiêu và sầu riêng; hồ tiêu bị cưa bỏ vì sâu bệnh.
 */
@IntegrationTest
class CultivationApiIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void intercroppingLifecycleAndLandUseHistory() throws Exception {
        long plotId = createPlot();
        long coffee = plant(plotId, 1, "2016-06-15", 1100, true);
        long pepper = plant(plotId, 3, "2019-07-01", 400, true);
        long durian = plant(plotId, 4, "2023-05-20", 80, false);

        // Epic B: lô đang có 3 cây sống cùng lúc
        mvc.perform(get("/api/v1/plots/{id}/plantings", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // Epic C: cưa bỏ hồ tiêu vì sâu bệnh
        mvc.perform(post("/api/v1/plantings/{id}/termination", pepper).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "2026-08-20", "reason": "PEST_DISEASE", "note": "Chết nhanh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));

        mvc.perform(get("/api/v1/plots/{id}/plantings", plotId))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.containsInAnyOrder((int) coffee, (int) durian)));
        mvc.perform(get("/api/v1/plots/{id}/plantings", plotId).param("activeOnly", "false"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.id == %d)].endReason".formatted(pepper)).value("PEST_DISEASE"));

        // BR-03: đã kết thúc thì không đổi được nữa
        mvc.perform(post("/api/v1/plantings/{id}/production-start", pepper))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-03"));

        // BR-10: lô có lịch sử canh tác không xóa được — kể cả khi lứa trồng đã kết thúc
        mvc.perform(delete("/api/v1/plots/{id}", plotId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("3 lứa trồng (2 đang canh tác)")));
    }

    /**
     * Sửa lại lứa trồng nhập sai (BR-02, BR-04). Khai nhầm ngày trồng hay số cây là chuyện
     * thường; nhưng sửa xong mà ngày trồng vượt qua ngày đã cưa bỏ thì cả vòng đời trở nên vô
     * nghĩa, nên đó là ranh giới không được phép bước qua.
     */
    @Test
    void br04_correctingAPlantingStaysInsideItsOwnLifetime() throws Exception {
        long plotId = createPlot();
        long plantingId = plant(plotId, 1, "2021-06-15", 400, true);

        mvc.perform(put("/api/v1/plantings/{id}", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plantingDate": "2021-07-01", "treeCount": 380}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantingDate").value("2021-07-01"))
                .andExpect(jsonPath("$.treeCount").value(380));

        // BR-02: ngày trồng ở tương lai là vô lý
        mvc.perform(put("/api/v1/plantings/{id}", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plantingDate": "2030-01-01", "treeCount": 380}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-02"));

        mvc.perform(post("/api/v1/plantings/{id}/termination", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "2024-03-10", "reason": "OLD_AGE"}
                                """))
                .andExpect(status().isOk());

        // BR-04: sau khi đã cưa bỏ, không thể dời ngày trồng vượt qua ngày kết thúc
        mvc.perform(put("/api/v1/plantings/{id}", plantingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plantingDate": "2024-06-01", "treeCount": 380}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-04"));
    }

    @Test
    void listingPlantingsDoesNotTriggerNPlusOneQueries() throws Exception {
        long plotId = createPlot();
        for (int i = 0; i < 5; i++) {
            plant(plotId, 1 + (i % 4), "2020-01-0" + (i + 1), 100, false);
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mvc.perform(get("/api/v1/plots/{id}/plantings", plotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        // 1 kiểm tra user + 1 kiểm tra quyền sở hữu lô + 1 lấy lứa trồng kèm lô và cây trồng.
        // Không có @EntityGraph sẽ là 3 + (số cây trồng khác nhau) truy vấn trở lên.
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    /**
     * Form ghi nhật ký chọn lứa trồng trong một lần gọi (plan M6 §6.1): gom mọi lô, bỏ lứa đã cưa,
     * lọc được theo nông trại, không lộ nông trại của người khác (BR-11), và không N+1.
     */
    @Test
    void listingEveryOwnedPlantingForTheLogForm() throws Exception {
        long farmId = createFarm("Nông trại một lần gọi");
        long plotB = createPlot(farmId, "Lô B1");
        long plotA = createPlot(farmId, "Lô A1");
        long coffee = plant(plotA, 1, "2016-06-15", 1100, true);
        long pepper = plant(plotA, 3, "2019-07-01", 400, true);
        long durian = plant(plotB, 4, "2023-05-20", 80, false);
        long otherFarmPlanting = plant(createPlot(createFarm("Nông trại khác"), "Lô Z"), 1, "2020-01-01", 10, true);
        mvc.perform(post("/api/v1/plantings/{id}/termination", pepper).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endDate\": \"2026-08-20\", \"reason\": \"PEST_DISEASE\"}"))
                .andExpect(status().isOk());

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        // Xếp theo tên lô (A1 trước B1), lứa đã cưa không có mặt
        mvc.perform(get("/api/v1/plantings").param("farmId", String.valueOf(farmId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.contains((int) coffee, (int) durian)))
                .andExpect(jsonPath("$[0].plotName").value("Lô A1"))
                .andExpect(jsonPath("$[0].cropName").value("Cà phê (Robusta)"));
        // 1 kiểm tra user + 1 kiểm tra quyền sở hữu nông trại + 1 lấy lứa trồng kèm lô và cây trồng
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);

        mvc.perform(get("/api/v1/plantings").param("farmId", String.valueOf(farmId)).param("activeOnly", "false"))
                .andExpect(jsonPath("$.length()").value(3));

        // Bỏ trống farmId: gom mọi nông trại của chủ sở hữu
        mvc.perform(get("/api/v1/plantings"))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.hasItems(
                        (int) coffee, (int) durian, (int) otherFarmPlanting)));

        // BR-11: người khác không thấy gì, và nông trại của mình trả 404 với họ
        mvc.perform(get("/api/v1/plantings").header("X-User-Id", 2))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem((int) coffee))));
        mvc.perform(get("/api/v1/plantings").param("farmId", String.valueOf(farmId)).header("X-User-Id", 2))
                .andExpect(status().isNotFound());
    }

    private long createFarm(String name) throws Exception {
        String farm = mvc.perform(post("/api/v1/farms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(farm, "$.id").longValue();
    }

    private long createPlot(long farmId, String name) throws Exception {
        String plot = mvc.perform(post("/api/v1/farms/{id}/plots", farmId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\", \"areaM2\": 10000}".formatted(name)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(plot, "$.id").longValue();
    }

    private long createPlot() throws Exception {
        String farm = mvc.perform(post("/api/v1/farms").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Nông trại xen canh\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long farmId = JsonPath.<Number>read(farm, "$.id").longValue();
        String plot = mvc.perform(post("/api/v1/farms/{id}/plots", farmId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lô A2\", \"areaM2\": 15000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(plot, "$.id").longValue();
    }

    private long plant(long plotId, long cropId, String date, int trees, boolean producing) throws Exception {
        String body = mvc.perform(post("/api/v1/plots/{id}/plantings", plotId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cropId": %d, "plantingDate": "%s", "treeCount": %d, "alreadyProducing": %s}
                                """.formatted(cropId, date, trees, producing)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }
}
