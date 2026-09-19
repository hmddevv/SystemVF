package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.ActivityView;
import com.hmdao.farm.cultivation.domain.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ActivityResponse(
        @Schema(example = "31") Long id,
        @Schema(description = "Niên vụ hệ thống tự gán theo ngày thực hiện", example = "7") Long seasonId,
        @Schema(example = "2025/2026") String seasonLabel,
        @Schema(example = "FERTILIZING") ActivityType type,
        @Schema(example = "2025-06-10") LocalDate activityDate,
        @Schema(example = "12500000.00") BigDecimal cost,
        @Schema(description = "Có thể null, trừ hoạt động loại OTHER", example = "Bón NPK 16-16-8, 20 bao")
        String note) {

    public static ActivityResponse from(ActivityView view) {
        return new ActivityResponse(view.id(), view.seasonId(), view.seasonLabel(), view.type(),
                view.activityDate(), view.cost(), view.note());
    }
}
