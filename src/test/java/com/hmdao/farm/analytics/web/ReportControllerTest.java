package com.hmdao.farm.analytics.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.analytics.application.dto.ProfitLossCriteria;
import com.hmdao.farm.analytics.application.dto.ProfitLossReport;
import com.hmdao.farm.analytics.application.dto.ProfitLossRow;
import com.hmdao.farm.analytics.application.port.in.ProfitLossReportUseCase;
import com.hmdao.farm.analytics.domain.ProfitLossGrouping;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProfitLossReportUseCase reports;

    @Test
    void defaultsToGroupingByCropAndNoFilters() throws Exception {
        when(reports.report(any())).thenReturn(
                new ProfitLossReport(ProfitLossGrouping.CROP, null, List.of(), null));

        mvc.perform(get("/api/v1/reports/profit-loss")).andExpect(status().isOk());

        verify(reports).report(new ProfitLossCriteria(ProfitLossGrouping.CROP, null, null));
    }

    @Test
    void passesGroupingYearAndFarmThrough() throws Exception {
        when(reports.report(any())).thenReturn(
                new ProfitLossReport(ProfitLossGrouping.PLANTING, 2025, List.of(), null));

        mvc.perform(get("/api/v1/reports/profit-loss")
                        .param("groupBy", "PLANTING").param("year", "2025").param("farmId", "3"))
                .andExpect(status().isOk());

        verify(reports).report(new ProfitLossCriteria(ProfitLossGrouping.PLANTING, 2025, 3L));
    }

    @Test
    void unknownGroupingIsRejectedAsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/reports/profit-loss").param("groupBy", "WEATHER"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reports);
    }

    @Test
    void rendersNormalisedFiguresAndTheTotalRow() throws Exception {
        ProfitLossRow coffee = new ProfitLossRow(1L, "Cà phê (Robusta)", 3, 14, new BigDecimal("75000000"),
                6, 4800, new BigDecimal("115200000"), new BigDecimal("40200000"), 20_000d, 1600,
                new BigDecimal("2010000.00"), new BigDecimal("25125.00"), 3.0, true,
                new BigDecimal("40200000"), 2025);
        when(reports.report(any())).thenReturn(
                new ProfitLossReport(ProfitLossGrouping.CROP, null, List.of(coffee), coffee));

        mvc.perform(get("/api/v1/reports/profit-loss"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupBy").value("CROP"))
                .andExpect(jsonPath("$.rows[0].label").value("Cà phê (Robusta)"))
                .andExpect(jsonPath("$.rows[0].profitPerTree").value(25125.00))
                .andExpect(jsonPath("$.rows[0].sharedPlot").value(true))
                .andExpect(jsonPath("$.rows[0].paybackYear").value(2025))
                .andExpect(jsonPath("$.total.netProfit").value(40200000));
    }
}
