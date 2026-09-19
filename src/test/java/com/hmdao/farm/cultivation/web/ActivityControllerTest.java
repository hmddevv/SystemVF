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

import com.hmdao.farm.cultivation.application.port.in.ActivityQueryUseCase;
import com.hmdao.farm.cultivation.application.port.in.LogActivityUseCase;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;
import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ActivityController.class)
class ActivityControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LogActivityUseCase logging;

    @MockitoBean
    ActivityQueryUseCase query;

    @Test
    void typeAndDateAreRequired() throws Exception {
        mvc.perform(post("/api/v1/plantings/5/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field")
                        .value(Matchers.containsInAnyOrder("type", "activityDate")));

        verifyNoInteractions(logging);
    }

    @Test
    void unknownActivityTypeIsRejectedAsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/plantings/5/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "DANCING", "activityDate": "2025-06-10"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(logging);
    }

    @Test
    void negativeCostIsRejectedBeforeReachingTheDomain() throws Exception {
        mvc.perform(post("/api/v1/plantings/5/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "WATERING", "activityDate": "2025-06-10", "cost": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(Matchers.hasItem("cost")));

        verifyNoInteractions(logging);
    }

    @Test
    void br12_missingNoteOnOtherBecomes422WithRuleCode() throws Exception {
        when(logging.log(eq(5L), any()))
                .thenThrow(new BusinessRuleViolationException("BR-12", "Hoạt động loại OTHER phải có ghi chú."));

        mvc.perform(post("/api/v1/plantings/5/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "OTHER", "activityDate": "2025-06-10"}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-12"));
    }

    @Test
    void listUsesDefaultPagingWhenNoParamsGiven() throws Exception {
        when(query.listBySeason(eq(7L), any())).thenReturn(new Page<>(List.of(), 0, 20, 0));

        mvc.perform(get("/api/v1/seasons/7/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(query).listBySeason(7L, PageRequest.of(0, 20));
    }

    @Test
    void pageSizeIsCappedSoOneRequestCannotPullTheWholeLog() throws Exception {
        when(query.listBySeason(eq(7L), any())).thenReturn(new Page<>(List.of(), 0, 200, 0));

        mvc.perform(get("/api/v1/seasons/7/activities").param("size", "5000"))
                .andExpect(status().isOk());

        verify(query).listBySeason(7L, PageRequest.of(0, 200));
    }
}
