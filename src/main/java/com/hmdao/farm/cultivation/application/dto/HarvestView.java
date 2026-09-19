package com.hmdao.farm.cultivation.application.dto;

import com.hmdao.farm.cultivation.domain.Harvest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * @param pricePerKg doanh thu chia sản lượng — giá bán thực tế của lần thu hoạch đó, con số
 *                   nhà nông nhìn đầu tiên để so với giá thị trường
 */
public record HarvestView(
        Long id,
        Long seasonId,
        String seasonLabel,
        LocalDate harvestDate,
        double quantityKg,
        BigDecimal revenue,
        BigDecimal pricePerKg) {

    /** Cần {@code season} đã được nạp (xem @EntityGraph ở adapter persistence). */
    public static HarvestView of(Harvest harvest) {
        return new HarvestView(
                harvest.getId(),
                harvest.getSeason().getId(),
                harvest.getSeason().getLabel(),
                harvest.getHarvestDate(),
                harvest.getQuantityKg(),
                harvest.getRevenue(),
                harvest.getRevenue().divide(BigDecimal.valueOf(harvest.getQuantityKg()), 2, RoundingMode.HALF_UP));
    }
}
