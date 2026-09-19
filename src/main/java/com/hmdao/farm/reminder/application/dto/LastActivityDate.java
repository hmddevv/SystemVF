package com.hmdao.farm.reminder.application.dto;

import com.hmdao.farm.cultivation.domain.ActivityType;
import java.time.LocalDate;

/** Ngày làm gần nhất của một loại việc trên một lứa trồng — một câu {@code MAX ... GROUP BY}. */
public record LastActivityDate(Long plantingId, ActivityType type, LocalDate lastDate) {
}
