package com.hmdao.farm.reminder.domain;

import com.hmdao.farm.cultivation.domain.ActivityType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Một việc chăm sóc nên làm cho một lứa trồng. Là giá trị tính ra từ nhật ký, không phải bản
 * ghi được lưu — ghi nhật ký tương ứng thì lời nhắc tự biến mất (BR-18, ADR-12).
 *
 * @param daysOverdue số ngày đã quá hạn; ≤ 0 nghĩa là còn bấy nhiêu ngày nữa mới tới hạn
 */
public record Reminder(
        String ruleCode,
        Long plantingId,
        String cropName,
        String plotName,
        ActivityType suggestedActivity,
        String title,
        String detail,
        LocalDate dueDate,
        ReminderSeverity severity,
        long daysOverdue) {

    /** Chỉ nhắc việc trong tầm một tuần — xa hơn thì danh sách thành nhiễu. */
    public static final int LOOKAHEAD_DAYS = 7;

    /**
     * Tạo lời nhắc nếu đã tới hoặc sắp tới hạn, ngược lại trả rỗng. Luật chỉ cần tính
     * {@code dueDate}, phần còn lại thống nhất ở đây.
     */
    public static Optional<Reminder> dueBy(CareContext context, String ruleCode, ActivityType activity,
            LocalDate dueDate, String title, String detail) {
        if (dueDate.isAfter(context.today().plusDays(LOOKAHEAD_DAYS))) {
            return Optional.empty();
        }
        long overdue = ChronoUnit.DAYS.between(dueDate, context.today());
        return Optional.of(new Reminder(ruleCode, context.plantingId(), context.cropName(), context.plotName(),
                activity, title, detail, dueDate,
                overdue > 0 ? ReminderSeverity.OVERDUE : ReminderSeverity.DUE_SOON, overdue));
    }
}
