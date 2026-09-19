package com.hmdao.farm.analytics.application.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Một dòng báo cáo lãi/lỗ.
 *
 * <p>Ba chỉ số chuẩn hoá ở cuối mới là thứ trả lời được "nơi nào canh tác hiệu quả" (BR-15):
 * lãi tuyệt đối thì lô 5 ha luôn thắng lô 1 ha. Thiếu diện tích hoặc số cây thì để trống chứ
 * không chia bừa.
 *
 * @param sharedPlot        nhóm có lô trồng xen — chi phí dùng chung được ghi vào từng lứa nên
 *                          số theo đơn vị diện tích chỉ là ước lượng (BR-16, ADR-10)
 * @param lifetimeNetProfit lãi/lỗ trên **toàn bộ** niên vụ, không chịu bộ lọc {@code year}
 * @param paybackYear       niên vụ đầu tiên luỹ kế hoà vốn; {@code null} = chưa hoàn vốn.
 *                          Chỉ có ý nghĩa khi gom theo {@code PLANTING} (BR-17)
 */
public record ProfitLossRow(
        Long id,
        String label,
        int seasonCount,
        long activityCount,
        BigDecimal totalCost,
        long harvestCount,
        double totalQuantityKg,
        BigDecimal totalRevenue,
        BigDecimal netProfit,
        Double areaM2,
        Integer treeCount,
        BigDecimal profitPer1000m2,
        BigDecimal profitPerTree,
        Double yieldKgPerTree,
        boolean sharedPlot,
        BigDecimal lifetimeNetProfit,
        Integer paybackYear) {

    private static final int MONEY_SCALE = 2;

    /** Chia tiền cho một đại lượng vật lý; trả {@code null} khi mẫu số không dùng được (BR-15). */
    public static BigDecimal per(BigDecimal amount, double divisor) {
        return divisor <= 0 ? null
                : amount.divide(BigDecimal.valueOf(divisor), MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
