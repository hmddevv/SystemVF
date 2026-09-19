package com.hmdao.farm.reminder.domain;

import com.hmdao.farm.cultivation.domain.ActivityType;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Mùa mưa (tháng 5 đến tháng 9) là cửa sổ bón phân: đất đủ ẩm thì cây mới hấp thụ được, bón
 * vào mùa khô mà không tưới kèm thì phân nằm trơ trên mặt đất. Thực tế chia 3–4 đợt, cách nhau
 * khoảng 45 ngày.
 */
public class RainySeasonFertilizingRule implements CareRule {

    static final int INTERVAL_DAYS = 45;
    private static final int RAINY_SEASON_FROM = 5;
    private static final int RAINY_SEASON_TO = 9;

    @Override
    public String code() {
        return "CARE-02";
    }

    @Override
    public Optional<Reminder> evaluate(CareContext context) {
        if (!context.todayInMonths(RAINY_SEASON_FROM, RAINY_SEASON_TO)) {
            return Optional.empty();
        }
        LocalDate due = context.nextDue(ActivityType.FERTILIZING, INTERVAL_DAYS);
        String detail = context.lastDone(ActivityType.FERTILIZING)
                .map(last -> "Đợt bón gần nhất %s, chu kỳ khuyến nghị %d ngày.".formatted(last, INTERVAL_DAYS))
                .orElse("Chưa có đợt bón phân nào được ghi trong mùa mưa này.");
        return Reminder.dueBy(context, code(), ActivityType.FERTILIZING, due, "Bón phân mùa mưa", detail);
    }
}
