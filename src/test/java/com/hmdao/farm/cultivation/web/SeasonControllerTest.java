package com.hmdao.farm.cultivation.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.cultivation.application.dto.SeasonView;
import com.hmdao.farm.cultivation.application.port.in.ManageSeasonUseCase;
import com.hmdao.farm.cultivation.application.port.in.SeasonQueryUseCase;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test tầng web cho niên vụ. Điểm đáng kiểm nhất là response mang sẵn tổng chi phí,
 * sản lượng và lãi/lỗ: đó là thứ khiến màn hình "so sánh các vụ" không phải gọi thêm API nào.
 */
@WebMvcTest(SeasonController.class)
class SeasonControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SeasonQueryUseCase query;

    @MockitoBean
    ManageSeasonUseCase management;

    @Test
    void aSeasonCarriesItsOwnProfitAndLossSoTheScreenNeedsNoSecondCall() throws Exception {
        when(query.listByPlanting(5L)).thenReturn(List.of(coffeeSeason(2025, "12500000", "96800000")));

        mvc.perform(get("/api/v1/plantings/5/seasons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("2025/2026"))
                .andExpect(jsonPath("$[0].totalCost").value(12_500_000))
                .andExpect(jsonPath("$[0].totalRevenue").value(96_800_000))
                .andExpect(jsonPath("$[0].netProfit").value(84_300_000));
    }

    @Test
    void aLosingSeasonShowsANegativeNetProfitRatherThanHidingIt() throws Exception {
        when(query.getById(9L)).thenReturn(coffeeSeason(2023, "40000000", "15000000"));

        mvc.perform(get("/api/v1/seasons/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netProfit").value(-25_000_000));
    }

    @Test
    void br10_deletingASeasonThatStillHasRecordsIs409() throws Exception {
        doThrow(new ResourceConflictException("BR-10", "Niên vụ còn 12 hoạt động và 3 lần thu hoạch."))
                .when(management).delete(eq(9L));

        mvc.perform(delete("/api/v1/seasons/9"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR-10"));
    }

    @Test
    void deletingAnEmptySeasonReturns204() throws Exception {
        mvc.perform(delete("/api/v1/seasons/9")).andExpect(status().isNoContent());

        verify(management).delete(9L);
    }

    @Test
    void br11_aSeasonOfAnotherOwnerIsReportedAsNotFound() throws Exception {
        when(query.getById(9L)).thenThrow(new ResourceNotFoundException("niên vụ", 9L));

        mvc.perform(get("/api/v1/seasons/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.rule").value("BR-11"));
    }

    private static SeasonView coffeeSeason(int year, String cost, String revenue) {
        BigDecimal totalCost = new BigDecimal(cost);
        BigDecimal totalRevenue = new BigDecimal(revenue);
        return new SeasonView(9L, 5L, "Cà phê (Robusta)", year, year + "/" + (year + 1),
                LocalDate.of(year, 2, 1), LocalDate.of(year + 1, 1, 31),
                12, totalCost, 3, 3200, totalRevenue, totalRevenue.subtract(totalCost));
    }
}
