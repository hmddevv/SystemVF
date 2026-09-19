package com.hmdao.farm.analytics.application.dto;

import com.hmdao.farm.analytics.domain.ProfitLossGrouping;

/**
 * @param year   niên vụ cần xem ({@code SEASON.year}, không phải năm dương lịch — BR-13);
 *               {@code null} = mọi niên vụ
 * @param farmId {@code null} = mọi nông trại của chủ sở hữu hiện tại
 */
public record ProfitLossCriteria(ProfitLossGrouping groupBy, Integer year, Long farmId) {

    public ProfitLossCriteria {
        groupBy = groupBy == null ? ProfitLossGrouping.CROP : groupBy;
    }
}
