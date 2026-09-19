package com.hmdao.farm.reminder.web.dto;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.reminder.application.dto.ReminderView;
import com.hmdao.farm.reminder.domain.ReminderSeverity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record ReminderResponse(
        @Schema(description = "Mã luật sinh ra lời nhắc, truy vết tới tài liệu", example = "CARE-01")
        String ruleCode,
        @Schema(example = "2") Long plantingId,
        @Schema(example = "Cà phê (Robusta)") String cropName,
        @Schema(example = "Lô A2") String plotName,
        @Schema(description = "Loại hoạt động nên ghi sau khi làm xong", example = "WATERING")
        ActivityType suggestedActivity,
        @Schema(example = "Tưới nước mùa khô") String title,
        @Schema(example = "Đợt tưới gần nhất 2026-01-05, chu kỳ khuyến nghị 20 ngày.") String detail,
        @Schema(example = "2026-01-25") LocalDate dueDate,
        @Schema(example = "OVERDUE") ReminderSeverity severity,
        @Schema(description = "Số ngày đã quá hạn; ≤ 0 là còn bấy nhiêu ngày nữa mới tới hạn",
                example = "6") long daysOverdue) {

    public static ReminderResponse from(ReminderView view) {
        return new ReminderResponse(view.ruleCode(), view.plantingId(), view.cropName(), view.plotName(),
                view.suggestedActivity(), view.title(), view.detail(), view.dueDate(), view.severity(),
                view.daysOverdue());
    }
}
