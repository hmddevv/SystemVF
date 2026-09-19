package com.hmdao.farm.reminder.domain;

import com.hmdao.farm.cultivation.domain.ActivityType;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Mùa khô Tây Nguyên (tháng 12 đến tháng 4) là lúc cây lâu năm sống nhờ nước tưới: cà phê
 * thiếu nước đúng đợt ra hoa thì mất trắng vụ đó. Chu kỳ tưới thực tế khoảng 20–25 ngày một đợt.
 *
 * <p>Mùa khô suy từ tháng trong năm, không gọi dịch vụ thời tiết — thêm một phụ thuộc mạng vào
 * đường đọc chỉ để biết điều mà lịch canh tác đã nói sẵn là không đáng.
 */
public class DrySeasonIrrigationRule implements CareRule {

    static final int INTERVAL_DAYS = 20;
    private static final int DRY_SEASON_FROM = 12;
    private static final int DRY_SEASON_TO = 4;

    @Override
    public String code() {
        return "CARE-01";
    }

    @Override
    public Optional<Reminder> evaluate(CareContext context) {
        if (!context.perennial() || !context.todayInMonths(DRY_SEASON_FROM, DRY_SEASON_TO)) {
            return Optional.empty();
        }
        LocalDate due = context.nextDue(ActivityType.WATERING, INTERVAL_DAYS);
        String detail = context.lastDone(ActivityType.WATERING)
                .map(last -> "Đợt tưới gần nhất %s, chu kỳ khuyến nghị %d ngày.".formatted(last, INTERVAL_DAYS))
                .orElse("Chưa có đợt tưới nào được ghi trong mùa khô này.");
        return Reminder.dueBy(context, code(), ActivityType.WATERING, due, "Tưới nước mùa khô", detail);
    }
}
