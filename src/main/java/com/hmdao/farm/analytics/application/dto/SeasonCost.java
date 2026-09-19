package com.hmdao.farm.analytics.application.dto;

import java.math.BigDecimal;

/** Chi phí của một (lứa trồng, niên vụ), lấy bằng một câu {@code GROUP BY} cho cả nông trại. */
public record SeasonCost(Long plantingId, Integer year, Long entries, BigDecimal totalCost) {

    public SeasonCost {
        entries = entries == null ? 0L : entries;
        totalCost = totalCost == null ? BigDecimal.ZERO : totalCost;
    }
}
