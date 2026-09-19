package com.hmdao.farm.reminder.web;

import com.hmdao.farm.reminder.application.port.in.CareReminderUseCase;
import com.hmdao.farm.reminder.web.dto.ReminderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "9. Nhắc việc", description = """
        Epic F (Phase 2) — việc chăm sóc nên làm, tính lại mỗi lần gọi từ chính nhật ký. \
        Không có nút "đã làm": ghi nhật ký tương ứng thì lời nhắc tự biến mất.""")
@RestController
@RequestMapping("/api/v1/reminders")
class ReminderController {

    private final CareReminderUseCase reminders;

    ReminderController(CareReminderUseCase reminders) {
        this.reminders = reminders;
    }

    @Operation(summary = "Việc chăm sóc đang tới hạn",
            description = "Quá hạn trước, rồi đến việc tới hạn trong 7 ngày. Chỉ tính lứa đang canh tác. "
                    + "farmId bỏ trống = mọi nông trại của bạn.")
    @GetMapping
    List<ReminderResponse> list(@RequestParam(required = false) Long farmId) {
        return reminders.list(farmId).stream().map(ReminderResponse::from).toList();
    }
}
