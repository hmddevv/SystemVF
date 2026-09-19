package com.hmdao.farm.cultivation.application.dto;

import com.hmdao.farm.cultivation.domain.Season;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một niên vụ kèm số tổng hợp — đủ để trả lời "vụ này lời hay lỗ" mà không cần tải toàn bộ
 * nhật ký và thu hoạch về.
 *
 * @param label     "2025/2026" khi niên vụ vắt qua hai năm dương lịch
 * @param netProfit doanh thu trừ chi phí; báo cáo đầy đủ theo cây/lô/năm thuộc Phase 2
 */
public record SeasonView(
        Long id,
        Long plantingId,
        String cropName,
        int year,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        long activityCount,
        BigDecimal totalCost,
        long harvestCount,
        double totalQuantityKg,
        BigDecimal totalRevenue,
        BigDecimal netProfit) {

    /** Cần {@code planting} và {@code crop} đã được nạp (xem @EntityGraph ở adapter persistence). */
    public static SeasonView of(Season season, ActivityTotals activities, HarvestTotals harvests) {
        return new SeasonView(
                season.getId(),
                season.getPlanting().getId(),
                season.getPlanting().getCrop().getDisplayName(),
                season.getYear(),
                season.getLabel(),
                season.getStartDate(),
                season.getEndDate(),
                activities.entries(),
                activities.totalCost(),
                harvests.entries(),
                harvests.totalQuantityKg(),
                harvests.totalRevenue(),
                harvests.totalRevenue().subtract(activities.totalCost()));
    }
}
