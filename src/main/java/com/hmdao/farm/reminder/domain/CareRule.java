package com.hmdao.farm.reminder.domain;

import java.util.Optional;

/**
 * Một luật nhắc việc. Thêm luật mới = thêm một implementation và một {@code @Bean};
 * {@code CareReminderService} nhận {@code List<CareRule>} nên không phải sửa (Open/Closed).
 *
 * <p>Luật là hàm thuần trên {@link CareContext}: không truy vấn, không biết hôm nay là ngày
 * nào ngoài {@code context.today()}. Nhờ vậy test chỉ cần dựng context, không cần database.
 */
public interface CareRule {

    /** Mã luật, truy vết được tới tài liệu và test (vd. {@code CARE-01}). */
    String code();

    Optional<Reminder> evaluate(CareContext context);
}
