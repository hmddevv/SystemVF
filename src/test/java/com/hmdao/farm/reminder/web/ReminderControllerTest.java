package com.hmdao.farm.reminder.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.reminder.application.dto.ReminderView;
import com.hmdao.farm.reminder.application.port.in.CareReminderUseCase;
import com.hmdao.farm.reminder.domain.ReminderSeverity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReminderController.class)
class ReminderControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CareReminderUseCase reminders;

    @Test
    void listsEveryFarmWhenNoFarmIsGiven() throws Exception {
        when(reminders.list(null)).thenReturn(List.of());

        mvc.perform(get("/api/v1/reminders")).andExpect(status().isOk());

        verify(reminders).list(null);
    }

    @Test
    void rendersTheReminderWithItsRuleCodeAndDeadline() throws Exception {
        when(reminders.list(3L)).thenReturn(List.of(new ReminderView("CARE-01", 2L, "Cà phê (Robusta)", "Lô A2",
                ActivityType.WATERING, "Tưới nước mùa khô", "Đợt tưới gần nhất 2026-01-05.",
                LocalDate.of(2026, 1, 25), ReminderSeverity.OVERDUE, 6)));

        mvc.perform(get("/api/v1/reminders").param("farmId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleCode").value("CARE-01"))
                .andExpect(jsonPath("$[0].suggestedActivity").value("WATERING"))
                .andExpect(jsonPath("$[0].severity").value("OVERDUE"))
                .andExpect(jsonPath("$[0].dueDate").value("2026-01-25"))
                .andExpect(jsonPath("$[0].daysOverdue").value(6));
    }
}
