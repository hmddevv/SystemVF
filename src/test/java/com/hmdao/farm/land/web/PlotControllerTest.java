package com.hmdao.farm.land.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.land.application.dto.PlotView;
import com.hmdao.farm.land.application.port.in.ManagePlotUseCase;
import com.hmdao.farm.land.application.port.in.PlotQueryUseCase;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test tầng web: mock input port, kiểm tra validation, mã HTTP và định dạng ProblemDetail.
 * Kịch bản lấy từ tiêu chí chấp nhận trong docs/design.md.
 */
@WebMvcTest(PlotController.class)
class PlotControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ManagePlotUseCase managePlot;

    @MockitoBean
    PlotQueryUseCase plotQuery;

    @Test
    void givenExistingFarm_whenAddingPlotWithValidArea_thenPlotIsCreated() throws Exception {
        when(managePlot.create(eq(1L), any()))
                .thenReturn(new PlotView(7L, 1L, "Lô A2", 15_000, 1.5, "Đất đỏ bazan"));

        mvc.perform(post("/api/v1/farms/1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lô A2", "areaM2": 15000, "soilType": "Đất đỏ bazan"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/plots/7"))
                .andExpect(jsonPath("$.areaHectares").value(1.5));
    }

    @Test
    void givenNonPositiveAreaOrBlankName_whenSaving_thenValidationErrorAndNothingSaved() throws Exception {
        mvc.perform(post("/api/v1/farms/1/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " ", "areaM2": 0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:farm:problem:validation"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'areaM2')].message").value("Diện tích phải lớn hơn 0 m²"));

        verifyNoInteractions(managePlot);
    }

    @Test
    void mapsNotFoundDomainExceptionTo404WithRuleCode() throws Exception {
        when(managePlot.create(eq(99L), any())).thenThrow(new ResourceNotFoundException("nông trại", 99L));

        mvc.perform(post("/api/v1/farms/99/plots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Lô A2", "areaM2": 100}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.rule").value("BR-11"))
                .andExpect(jsonPath("$.detail").value("Không tìm thấy nông trại với id 99."));
    }
}
