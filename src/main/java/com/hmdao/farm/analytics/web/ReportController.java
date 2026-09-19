package com.hmdao.farm.analytics.web;

import com.hmdao.farm.analytics.application.dto.ProfitLossCriteria;
import com.hmdao.farm.analytics.application.port.in.ProfitLossReportUseCase;
import com.hmdao.farm.analytics.domain.ProfitLossGrouping;
import com.hmdao.farm.analytics.web.dto.ProfitLossReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. Báo cáo", description = """
        Epic E (Phase 2) — lãi/lỗ theo cây, lô hoặc từng lứa trồng. Gom theo NIÊN VỤ, \
        không theo năm dương lịch, nên chi phí chăm sóc và doanh thu của cùng một chu kỳ \
        sản xuất luôn nằm chung một dòng.""")
@RestController
@RequestMapping("/api/v1/reports")
class ReportController {

    private final ProfitLossReportUseCase reports;

    ReportController(ProfitLossReportUseCase reports) {
        this.reports = reports;
    }

    @Operation(summary = "Lãi/lỗ theo cây, lô hoặc lứa trồng",
            description = "year là niên vụ (vụ 2/2025–1/2026 là 2025), bỏ trống = mọi niên vụ. "
                    + "farmId bỏ trống = mọi nông trại của bạn. Lứa trồng chưa ghi gì vẫn hiện với số 0. "
                    + "groupBy=PLANTING kèm niên vụ hoàn vốn.")
    @GetMapping("/profit-loss")
    ProfitLossReportResponse profitLoss(
            @RequestParam(defaultValue = "CROP") ProfitLossGrouping groupBy,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long farmId) {
        return ProfitLossReportResponse.from(reports.report(new ProfitLossCriteria(groupBy, year, farmId)));
    }
}
