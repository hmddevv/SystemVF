package com.hmdao.farm.reminder.domain;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Vườn kiến thiết cơ bản chưa cho thu hoạch nên dễ bị bỏ quên — nhưng đây đúng là giai đoạn
 * quyết định bộ tán và bộ rễ của cả chục năm sau. Một tháng không có bản ghi nào là dấu hiệu
 * vườn đang bị bỏ trống.
 */
public class YoungOrchardCheckRule implements CareRule {

    static final int QUIET_DAYS = 30;
    static final int YOUNG_UNTIL_MONTHS = 36;

    @Override
    public String code() {
        return "CARE-04";
    }

    @Override
    public Optional<Reminder> evaluate(CareContext context) {
        if (context.status() != PlantingStatus.GROWING || context.ageMonths() > YOUNG_UNTIL_MONTHS) {
            return Optional.empty();
        }
        LocalDate lastTouched = context.lastDoneAnything().orElse(context.plantingDate());
        return Reminder.dueBy(context, code(), ActivityType.OTHER, lastTouched.plusDays(QUIET_DAYS),
                "Kiểm tra vườn kiến thiết cơ bản",
                "Vườn %d tháng tuổi, bản ghi gần nhất là ngày %s.".formatted(context.ageMonths(), lastTouched));
    }
}
