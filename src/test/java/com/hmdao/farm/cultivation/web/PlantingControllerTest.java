package com.hmdao.farm.cultivation.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.cultivation.application.port.in.CorrectPlantingUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantCropUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantingLifecycleUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantingQueryUseCase;
import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlantingController.class)
class PlantingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlantCropUseCase plantCrop;

    @MockitoBean
    PlantingLifecycleUseCase lifecycle;

    @MockitoBean
    CorrectPlantingUseCase correction;

    @MockitoBean
    PlantingQueryUseCase query;

    @Test
    void terminationRequiresDateAndKnownReason() throws Exception {
        mvc.perform(post("/api/v1/plantings/2/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.containsInAnyOrder("endDate", "reason")));

        verifyNoInteractions(lifecycle);
    }

    @Test
    void unknownEndReasonIsRejectedAsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/plantings/2/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "2026-08-20", "reason": "BORED"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lifecycle);
    }

    @Test
    void businessRuleViolationBecomes422WithRuleCode() throws Exception {
        when(lifecycle.terminate(eq(2L), any()))
                .thenThrow(new BusinessRuleViolationException("BR-03", "Lứa trồng đã kết thúc ngày 2026-08-20."));

        mvc.perform(post("/api/v1/plantings/2/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "2026-08-21", "reason": "OTHER"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-03"));
    }

    @Test
    void listDefaultsToActivePlantingsOnly() throws Exception {
        when(query.listByPlot(1L, true)).thenReturn(List.of());

        mvc.perform(get("/api/v1/plots/1/plantings")).andExpect(status().isOk());

        verify(query).listByPlot(1L, true);
    }
}
