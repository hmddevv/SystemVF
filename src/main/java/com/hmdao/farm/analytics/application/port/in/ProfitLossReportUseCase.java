package com.hmdao.farm.analytics.application.port.in;

import com.hmdao.farm.analytics.application.dto.ProfitLossCriteria;
import com.hmdao.farm.analytics.application.dto.ProfitLossReport;

/** Epic E (Phase 2) — lãi/lỗ theo cây, lô hoặc từng lứa trồng. */
public interface ProfitLossReportUseCase {

    ProfitLossReport report(ProfitLossCriteria criteria);
}
