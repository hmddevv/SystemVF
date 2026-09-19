package com.hmdao.farm.analytics.application.dto;

import java.math.BigDecimal;

/** Sản lượng và doanh thu của một (lứa trồng, niên vụ). Xem {@link SeasonCost}. */
public record SeasonYield(Long plantingId, Integer year, Long entries, Double quantityKg, BigDecimal revenue) {

    public SeasonYield {
        entries = entries == null ? 0L : entries;
        quantityKg = quantityKg == null ? 0d : quantityKg;
        revenue = revenue == null ? BigDecimal.ZERO : revenue;
    }
}
