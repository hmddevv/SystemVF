package com.hmdao.farm.reminder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import com.hmdao.farm.reminder.application.dto.LastActivityDate;
import com.hmdao.farm.reminder.application.dto.LastHarvestDate;
import com.hmdao.farm.reminder.application.dto.PlantingSnapshot;
import com.hmdao.farm.reminder.application.dto.ReminderView;
import com.hmdao.farm.reminder.application.port.out.CareContextQueryPort;
import com.hmdao.farm.reminder.domain.CareRule;
import com.hmdao.farm.reminder.domain.DrySeasonIrrigationRule;
import com.hmdao.farm.reminder.domain.PostHarvestPruningRule;
import com.hmdao.farm.reminder.domain.RainySeasonFertilizingRule;
import com.hmdao.farm.reminder.domain.ReminderSeverity;
import com.hmdao.farm.reminder.domain.YoungOrchardCheckRule;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Engine chạy với đúng bộ luật thật: mock chỉ thay phần đọc dữ liệu, còn quyết định nhắc gì
 * vẫn do luật đưa ra — nếu không thì test chỉ kiểm tra chính cái mock.
 */
class CareReminderServiceTest {

    private static final Long OWNER = 1L;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 15/6/2026 — giữa mùa mưa, nên luật tưới mùa khô phải im lặng. */
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    private final CareContextQueryPort query = mock(CareContextQueryPort.class);
    private final List<CareRule> rules = List.of(new DrySeasonIrrigationRule(),
            new RainySeasonFertilizingRule(), new PostHarvestPruningRule(), new YoungOrchardCheckRule());
    private final Clock clock = Clock.fixed(TODAY.atTime(0, 30).atZone(VIETNAM).toInstant(), VIETNAM);
    private final CareReminderService service = new CareReminderService(query, rules, () -> OWNER, clock);

    private void farmWithOneMatureAndOneYoungOrchard() {
        when(query.findActivePlantings(any(), any())).thenReturn(List.of(
                new PlantingSnapshot(1L, "Cà phê", "Robusta", "Lô A2", true, LocalDate.of(2016, 6, 15),
                        PlantingStatus.PRODUCING, 1100),
                new PlantingSnapshot(2L, "Sầu riêng", "Ri6", "Lô C", true, LocalDate.of(2025, 12, 15),
                        PlantingStatus.GROWING, 80)));
        when(query.lastActivityDates(any())).thenReturn(List.of(
                new LastActivityDate(1L, ActivityType.FERTILIZING, LocalDate.of(2026, 6, 5))));
        when(query.lastHarvestDates(any())).thenReturn(List.of(
                new LastHarvestDate(1L, LocalDate.of(2026, 5, 6))));
    }

    @Test
    void buildsContextPerPlantingAndRunsEveryRuleOnIt() {
        farmWithOneMatureAndOneYoungOrchard();

        List<ReminderView> reminders = service.list(null);

        assertThat(reminders).extracting(ReminderView::ruleCode)
                // CARE-01 vắng mặt vì đang mùa mưa; vườn cà phê vừa bón phân nên CARE-02 chỉ nhắc vườn sầu riêng
                .containsExactly("CARE-04", "CARE-03", "CARE-02");
    }

    @Test
    void overdueComesFirstAndTheOldestDeadlineLeads() {
        farmWithOneMatureAndOneYoungOrchard();

        List<ReminderView> reminders = service.list(null);

        assertThat(reminders.get(0).severity()).isEqualTo(ReminderSeverity.OVERDUE);
        assertThat(reminders.get(0).plotName()).isEqualTo("Lô C");
        assertThat(reminders.get(1).severity()).isEqualTo(ReminderSeverity.OVERDUE);
        assertThat(reminders.get(1).daysOverdue()).isEqualTo(19);
        assertThat(reminders.get(2).severity()).isEqualTo(ReminderSeverity.DUE_SOON);
    }

    @Test
    void wiresTheLatestEntryOfEachKindIntoTheRules() {
        farmWithOneMatureAndOneYoungOrchard();

        List<ReminderView> reminders = service.list(null);

        ReminderView pruning = reminders.stream()
                .filter(reminder -> reminder.ruleCode().equals("CARE-03")).findFirst().orElseThrow();
        assertThat(pruning.cropName()).isEqualTo("Cà phê (Robusta)");
        assertThat(pruning.detail()).contains("2026-05-06");
        assertThat(pruning.suggestedActivity()).isEqualTo(ActivityType.PRUNING);
    }

    @Test
    void br11_scopesEverythingToTheCurrentOwnerAndTheRequestedFarm() {
        farmWithOneMatureAndOneYoungOrchard();
        when(query.farmBelongsToOwner(OWNER, 7L)).thenReturn(true);

        service.list(7L);

        verify(query).findActivePlantings(OWNER, 7L);
    }

    /**
     * Lọc theo nông trại của người khác phải là 404, không phải danh sách rỗng: "không có việc
     * gì tới hạn" và "nông trại này không phải của bạn" là hai câu trả lời khác hẳn nhau.
     */
    @Test
    void br11_aFarmThatIsNotMineIsReportedAsNotFoundRatherThanAsAnEmptyList() {
        when(query.farmBelongsToOwner(OWNER, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.list(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("7");

        verify(query, never()).findActivePlantings(any(), any());
    }

    @Test
    void skipsTheAggregateQueriesEntirelyWhenNothingIsUnderCultivation() {
        when(query.findActivePlantings(any(), any())).thenReturn(List.of());

        assertThat(service.list(null)).isEmpty();

        verify(query).findActivePlantings(OWNER, null);
        verifyNoMoreInteractions(query);
    }
}
