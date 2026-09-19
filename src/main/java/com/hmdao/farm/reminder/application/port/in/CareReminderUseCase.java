package com.hmdao.farm.reminder.application.port.in;

import com.hmdao.farm.reminder.application.dto.ReminderView;
import java.util.List;

/** Epic F (Phase 2) — việc chăm sóc nên làm, tính lại mỗi lần gọi từ nhật ký (ADR-12). */
public interface CareReminderUseCase {

    /** @param farmId {@code null} = mọi nông trại của chủ sở hữu hiện tại */
    List<ReminderView> list(Long farmId);
}
