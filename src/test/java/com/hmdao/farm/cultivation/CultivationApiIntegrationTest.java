package com.hmdao.farm.cultivation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Kịch bản thực tế: lô cà phê trồng xen hồ tiêu và sầu riêng; hồ tiêu bị cưa bỏ vì sâu bệnh.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CultivationApiIntegrationTest {

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
