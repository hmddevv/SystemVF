package com.hmdao.farm.cultivation.application.dto;

import com.hmdao.farm.cultivation.domain.ActivityType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** {@code cost} để trống nghĩa là 0 — việc tự làm, không tốn chi phí (BR-08). */
public record LogActivityCommand(ActivityType type, LocalDate activityDate, BigDecimal cost, String note) {
}
