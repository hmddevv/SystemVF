package com.hmdao.farm.reminder.domain;

import com.hmdao.farm.cultivation.domain.ActivityType;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Sau thu hoạch, cây kiệt sức và mang đầy cành khô, cành tăm. Tỉa sớm để cây dồn dinh dưỡng
 * cho cành cho trái vụ sau; để muộn là mất một phần năng suất năm tới.
 *
 * <p>Luật tự tắt khi đã tỉa sau lần thu hoạch gần nhất — đó là toàn bộ cơ chế "đánh dấu đã
 * làm" mà hệ thống cần (BR-18).
 */
public class PostHarvestPruningRule implements CareRule {

    static final int GRACE_DAYS = 21;

    @Override
    public String code() {
        return "CARE-03";
    }

    @Override
    public Optional<Reminder> evaluate(CareContext context) {
        LocalDate lastHarvest = context.lastHarvest();
        if (!context.perennial() || lastHarvest == null) {
            return Optional.empty();
        }
        boolean prunedSinceHarvest = context.lastDone(ActivityType.PRUNING)
                .filter(pruned -> !pruned.isBefore(lastHarvest))
                .isPresent();
        if (prunedSinceHarvest) {
            return Optional.empty();
        }
        return Reminder.dueBy(context, code(), ActivityType.PRUNING, lastHarvest.plusDays(GRACE_DAYS),
                "Tỉa cành sau thu hoạch",
                "Thu hoạch gần nhất ngày %s, chưa ghi nhận đợt tỉa cành nào sau đó.".formatted(lastHarvest));
    }
}
