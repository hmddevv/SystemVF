package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.SeasonView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SeasonResponse(
        @Schema(example = "7") Long id,
        @Schema(example = "2") Long plantingId,
        @Schema(example = "Cà phê (Robusta)") String cropName,
        @Schema(description = "Năm bắt đầu niên vụ", example = "2025") int year,
        @Schema(description = "Nhãn hiển thị; vắt qua hai năm thì có dạng 2025/2026", example = "2025/2026")
        String label,
        @Schema(example = "2025-02-01") LocalDate startDate,
        @Schema(description = "null khi niên vụ đang diễn ra và chưa có cận trên", example = "2026-01-31")
        LocalDate endDate,
        @Schema(example = "12") long activityCount,
        @Schema(description = "Tổng chi phí chăm sóc trong niên vụ", example = "48250000.00") BigDecimal totalCost,
        @Schema(example = "3") long harvestCount,
        @Schema(example = "5400.5") double totalQuantityKg,
        @Schema(example = "129600000.00") BigDecimal totalRevenue,
        @Schema(description = "Doanh thu trừ chi phí; báo cáo đầy đủ theo cây/lô/năm thuộc Phase 2",
                example = "81350000.00") BigDecimal netProfit) {

    public static SeasonResponse from(SeasonView view) {
        return new SeasonResponse(view.id(), view.plantingId(), view.cropName(), view.year(), view.label(),
                view.startDate(), view.endDate(), view.activityCount(), view.totalCost(), view.harvestCount(),
                view.totalQuantityKg(), view.totalRevenue(), view.netProfit());
    }
}
