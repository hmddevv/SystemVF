package com.hmdao.farm.analytics.web.dto;

import com.hmdao.farm.analytics.application.dto.ProfitLossReport;
import com.hmdao.farm.analytics.domain.ProfitLossGrouping;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProfitLossReportResponse(
        @Schema(example = "CROP") ProfitLossGrouping groupBy,
        @Schema(description = "Niên vụ đang lọc; null = mọi niên vụ", example = "2025") Integer year,
        @Schema(description = "Sắp xếp theo lãi/lỗ giảm dần") List<ProfitLossRowResponse> rows,
        @Schema(description = "Dòng tổng của toàn báo cáo; null khi chưa có lứa trồng nào")
        ProfitLossRowResponse total) {

    public static ProfitLossReportResponse from(ProfitLossReport report) {
        return new ProfitLossReportResponse(
                report.groupBy(),
                report.year(),
                report.rows().stream().map(ProfitLossRowResponse::from).toList(),
                report.total() == null ? null : ProfitLossRowResponse.from(report.total()));
    }
}
