package com.hmdao.farm.analytics.web.dto;

import com.hmdao.farm.analytics.application.dto.ProfitLossRow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ProfitLossRowResponse(
        @Schema(description = "Id của cây trồng, lô đất hoặc lứa trồng tùy groupBy; null ở dòng tổng",
                example = "1") Long id,
        @Schema(example = "Cà phê (Robusta)") String label,
        @Schema(description = "Số niên vụ có dữ liệu trong phạm vi lọc", example = "5") int seasonCount,
        @Schema(example = "37") long activityCount,
        @Schema(example = "241500000.00") BigDecimal totalCost,
        @Schema(example = "9") long harvestCount,
        @Schema(example = "16800.5") double totalQuantityKg,
        @Schema(example = "403200000.00") BigDecimal totalRevenue,
        @Schema(example = "161700000.00") BigDecimal netProfit,
        @Schema(description = "Tổng diện tích các lô liên quan, không cộng trùng lô trồng xen",
                example = "15000.0") Double areaM2,
        @Schema(example = "1100") Integer treeCount,
        @Schema(description = "Lãi/lỗ trên 1.000 m²; null khi không có diện tích hợp lệ",
                example = "10780000.00") BigDecimal profitPer1000m2,
        @Schema(description = "Lãi/lỗ trên mỗi cây; null khi không có số cây hợp lệ",
                example = "147000.00") BigDecimal profitPerTree,
        @Schema(example = "15.27") Double yieldKgPerTree,
        @Schema(description = "true khi nhóm có lô trồng xen — chi phí dùng chung ghi vào từng lứa "
                + "nên số theo diện tích chỉ là ước lượng", example = "true") boolean sharedPlot,
        @Schema(description = "Lãi/lỗ trên toàn bộ niên vụ, không chịu bộ lọc year",
                example = "161700000.00") BigDecimal lifetimeNetProfit,
        @Schema(description = "Niên vụ đầu tiên luỹ kế hòa vốn; null = chưa hoàn vốn. "
                + "Chỉ có khi groupBy=PLANTING", example = "2021") Integer paybackYear) {

    public static ProfitLossRowResponse from(ProfitLossRow row) {
        return new ProfitLossRowResponse(row.id(), row.label(), row.seasonCount(), row.activityCount(),
                row.totalCost(), row.harvestCount(), row.totalQuantityKg(), row.totalRevenue(), row.netProfit(),
                row.areaM2(), row.treeCount(), row.profitPer1000m2(), row.profitPerTree(), row.yieldKgPerTree(),
                row.sharedPlot(), row.lifetimeNetProfit(), row.paybackYear());
    }
}
