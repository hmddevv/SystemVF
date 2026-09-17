package com.hmdao.farm.land;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Chạy toàn bộ chuỗi Controller → Service → Spring Data → PostgreSQL (Testcontainers).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LandApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void farmLifecycleWithPlotsSummaryAndDeletionGuard() throws Exception {
        long farmId = createFarm("Nông trại tích hợp", 1);

        mvc.perform(post("/api/v1/farms/{id}/plots", farmId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lô A2", "areaM2": 15000, "soilType": "Đất đỏ bazan"}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/farms/{id}/plots", farmId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "lô a2", "areaM2": 500}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR-01"));

        // Tổng hợp số lô và diện tích bằng JPQL GROUP BY
        mvc.perform(get("/api/v1/farms/{id}", farmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plotCount").value(1))
                .andExpect(jsonPath("$.totalAreaM2").value(15000.0));

        mvc.perform(delete("/api/v1/farms/{id}", farmId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR-10"));
    }

    @Test
    void br11_ownersCannotSeeEachOthersFarms() throws Exception {
        long farmId = createFarm("Nông trại riêng của user 1", 1);

        mvc.perform(get("/api/v1/farms/{id}", farmId).header("X-User-Id", "2"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/farms/{id}", farmId).header("X-User-Id", "2"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/farms/{id}", farmId).header("X-User-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void seededCropCatalogIsAvailable() throws Exception {
        mvc.perform(get("/api/v1/crops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.displayName == 'Cà phê (Robusta)')].seasonStartMonth").value(2));
    }

    private long createFarm(String name, long ownerId) throws Exception {
        String body = mvc.perform(post("/api/v1/farms").header("X-User-Id", String.valueOf(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Number>read(body, "$.id").longValue();
    }
}
