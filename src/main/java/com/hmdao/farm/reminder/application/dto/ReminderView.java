package com.hmdao.farm.reminder.application.dto;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.reminder.domain.Reminder;
import com.hmdao.farm.reminder.domain.ReminderSeverity;
import java.time.LocalDate;

public record ReminderView(
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

    public static ReminderView of(Reminder reminder) {
        return new ReminderView(reminder.ruleCode(), reminder.plantingId(), reminder.cropName(),
                reminder.plotName(), reminder.suggestedActivity(), reminder.title(), reminder.detail(),
                reminder.dueDate(), reminder.severity(), reminder.daysOverdue());
    }
}
