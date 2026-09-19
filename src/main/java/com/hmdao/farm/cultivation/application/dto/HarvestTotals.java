package com.hmdao.farm.cultivation.application.dto;

import java.math.BigDecimal;

/** Tổng sản lượng và doanh thu của một niên vụ. Xem {@link ActivityTotals} về cách truy vấn. */
public record HarvestTotals(Long seasonId, Long entries, Double totalQuantityKg, BigDecimal totalRevenue) {

    public HarvestTotals {
        entries = entries == null ? 0L : entries;
        totalQuantityKg = totalQuantityKg == null ? 0d : totalQuantityKg;
        totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
    }

    public static HarvestTotals empty(Long seasonId) {
        return new HarvestTotals(seasonId, 0L, 0d, BigDecimal.ZERO);
    }
}
