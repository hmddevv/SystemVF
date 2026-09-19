package com.hmdao.farm.cultivation.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.cultivation.application.dto.HarvestView;
import com.hmdao.farm.cultivation.application.port.in.HarvestQueryUseCase;
import com.hmdao.farm.cultivation.application.port.in.RecordHarvestUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HarvestController.class)
class HarvestControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RecordHarvestUseCase recording;

    @MockitoBean
    HarvestQueryUseCase query;

    @Test
    void recordingReturns201WithLocationOfTheNewHarvest() throws Exception {
        when(recording.record(eq(5L), any())).thenReturn(new HarvestView(12L, 7L, "2025/2026",
                LocalDate.of(2025, 11, 28), 3200, new BigDecimal("76800000.00"), new BigDecimal("24000.00")));

        mvc.perform(post("/api/v1/plantings/5/harvests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"harvestDate": "2025-11-28", "quantityKg": 3200, "revenue": 76800000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/harvests/12"))
                .andExpect(jsonPath("$.seasonLabel").value("2025/2026"))
                .andExpect(jsonPath("$.pricePerKg").value(24000.00));
    }

    @Test
    void br08_nonPositiveQuantityIsRejectedBeforeReachingTheDomain() throws Exception {
        mvc.perform(post("/api/v1/plantings/5/harvests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"harvestDate": "2025-11-28", "quantityKg": 0, "revenue": 1000}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(Matchers.hasItem("quantityKg")));

        verifyNoInteractions(recording);
    }

    @Test
    void dateAndQuantityAreRequired() throws Exception {
        mvc.perform(post("/api/v1/plantings/5/harvests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field")
                        .value(Matchers.containsInAnyOrder("harvestDate", "quantityKg")));

        verifyNoInteractions(recording);
    }
}
