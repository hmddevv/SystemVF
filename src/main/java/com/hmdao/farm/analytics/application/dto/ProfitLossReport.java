package com.hmdao.farm.analytics.application.dto;

import com.hmdao.farm.analytics.domain.ProfitLossGrouping;
import java.util.List;

/**
 * @param total dòng tổng của toàn báo cáo; {@code null} khi chưa có lứa trồng nào trong phạm vi
 */
public record ProfitLossReport(ProfitLossGrouping groupBy, Integer year, List<ProfitLossRow> rows,
        ProfitLossRow total) {

    public ProfitLossReport {
        rows = List.copyOf(rows);
    }

    public static ProfitLossReport empty(ProfitLossCriteria criteria) {
        return new ProfitLossReport(criteria.groupBy(), criteria.year(), List.of(), null);
    }
}
