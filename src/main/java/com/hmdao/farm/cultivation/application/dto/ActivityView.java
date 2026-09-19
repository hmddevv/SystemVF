package com.hmdao.farm.cultivation.application.dto;

import com.hmdao.farm.cultivation.domain.Activity;
import com.hmdao.farm.cultivation.domain.ActivityType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Niên vụ đi kèm để người dùng thấy ngay dòng nhật ký được xếp vào vụ nào (BR-05a). */
public record ActivityView(
        Long id,
        Long seasonId,
        String seasonLabel,
        ActivityType type,
        LocalDate activityDate,
        BigDecimal cost,
        String note) {

    /** Cần {@code season} đã được nạp (xem @EntityGraph ở adapter persistence). */
    public static ActivityView of(Activity activity) {
        return new ActivityView(
                activity.getId(),
                activity.getSeason().getId(),
                activity.getSeason().getLabel(),
                activity.getType(),
                activity.getActivityDate(),
                activity.getCost(),
                activity.getNote());
    }
}
