package com.hmdao.farm.reminder.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Luật nhắc việc là hàm thuần trên {@link CareContext} nên test không cần Spring hay database —
 * đó chính là lý do engine dựng context sẵn thay vì để từng luật tự truy vấn.
 */
class CareRuleTest {

    /** Giữa mùa khô Tây Nguyên. */
    private static final LocalDate DRY_DAY = LocalDate.of(2026, 1, 15);
    /** Giữa mùa mưa. */
    private static final LocalDate RAINY_DAY = LocalDate.of(2026, 6, 15);

    @Nested
    class DrySeasonIrrigation {

        private final CareRule rule = new DrySeasonIrrigationRule();

        @Test
        void remindsWhenTheIrrigationCycleHasLapsed() {
            Optional<Reminder> reminder = rule.evaluate(perennial(DRY_DAY)
                    .watered(DRY_DAY.minusDays(25))
                    .build());

            assertThat(reminder).isPresent();
            assertThat(reminder.get().severity()).isEqualTo(ReminderSeverity.OVERDUE);
            assertThat(reminder.get().daysOverdue()).isEqualTo(5);
            assertThat(reminder.get().suggestedActivity()).isEqualTo(ActivityType.WATERING);
            assertThat(reminder.get().ruleCode()).isEqualTo("CARE-01");
        }

        @Test
        void warnsAWeekBeforeTheCycleIsUp() {
            Optional<Reminder> reminder = rule.evaluate(perennial(DRY_DAY)
                    .watered(DRY_DAY.minusDays(15))
                    .build());

            assertThat(reminder).isPresent();
            assertThat(reminder.get().severity()).isEqualTo(ReminderSeverity.DUE_SOON);
            assertThat(reminder.get().dueDate()).isEqualTo(DRY_DAY.plusDays(5));
        }

        @Test
        void staysQuietWhenTheOrchardWasWateredRecently() {
            assertThat(rule.evaluate(perennial(DRY_DAY).watered(DRY_DAY.minusDays(3)).build())).isEmpty();
        }

        @Test
        void anOrchardWithNoIrrigationRecordIsDueRightAway() {
            Optional<Reminder> reminder = rule.evaluate(perennial(DRY_DAY).build());

            assertThat(reminder).isPresent();
            assertThat(reminder.get().dueDate()).isEqualTo(DRY_DAY);
            assertThat(reminder.get().detail()).contains("Chưa có đợt tưới nào");
        }

        @Test
        void doesNotApplyOutsideTheDrySeason() {
            assertThat(rule.evaluate(perennial(RAINY_DAY).build())).isEmpty();
        }

        @Test
        void doesNotApplyToAnnualCrops() {
            assertThat(rule.evaluate(annual(DRY_DAY).build())).isEmpty();
        }
    }

    @Nested
    class RainySeasonFertilizing {

        private final CareRule rule = new RainySeasonFertilizingRule();

        @Test
        void remindsWhenTheFertilizingCycleHasLapsed() {
            Optional<Reminder> reminder = rule.evaluate(perennial(RAINY_DAY)
                    .did(ActivityType.FERTILIZING, RAINY_DAY.minusDays(50))
                    .build());

            assertThat(reminder).isPresent();
            assertThat(reminder.get().severity()).isEqualTo(ReminderSeverity.OVERDUE);
            assertThat(reminder.get().daysOverdue()).isEqualTo(5);
        }

        @Test
        void appliesToAnnualCropsTooBecauseTheyAlsoNeedFeedingInTheRains() {
            assertThat(rule.evaluate(annual(RAINY_DAY).build())).isPresent();
        }

        @Test
        void doesNotApplyInTheDrySeasonWhenFertiliserWouldJustSitOnDryGround() {
            assertThat(rule.evaluate(perennial(DRY_DAY).build())).isEmpty();
        }
    }

    @Nested
    class PostHarvestPruning {

        private final CareRule rule = new PostHarvestPruningRule();

        @Test
        void remindsAfterTheGracePeriodFollowingAHarvest() {
            Optional<Reminder> reminder = rule.evaluate(perennial(RAINY_DAY)
                    .harvested(RAINY_DAY.minusDays(40))
                    .build());

            assertThat(reminder).isPresent();
            assertThat(reminder.get().suggestedActivity()).isEqualTo(ActivityType.PRUNING);
            assertThat(reminder.get().daysOverdue()).isEqualTo(19);
        }

        @Test
        void br18_recordingThePruningIsWhatMakesTheReminderGoAway() {
            CareContext beforePruning = perennial(RAINY_DAY).harvested(RAINY_DAY.minusDays(40)).build();
            CareContext afterPruning = perennial(RAINY_DAY)
                    .harvested(RAINY_DAY.minusDays(40))
                    .did(ActivityType.PRUNING, RAINY_DAY.minusDays(5))
                    .build();

            assertThat(rule.evaluate(beforePruning)).isPresent();
            assertThat(rule.evaluate(afterPruning)).isEmpty();
        }

        @Test
        void pruningDoneBeforeTheHarvestDoesNotCount() {
            CareContext context = perennial(RAINY_DAY)
                    .harvested(RAINY_DAY.minusDays(40))
                    .did(ActivityType.PRUNING, RAINY_DAY.minusDays(60))
                    .build();

            assertThat(rule.evaluate(context)).isPresent();
        }

        @Test
        void staysQuietForAnOrchardThatHasNeverBeenHarvested() {
            assertThat(rule.evaluate(perennial(RAINY_DAY).build())).isEmpty();
        }
    }

    @Nested
    class YoungOrchardCheck {

        private final CareRule rule = new YoungOrchardCheckRule();

        @Test
        void remindsWhenAYoungOrchardHasBeenQuietForAMonth() {
            Optional<Reminder> reminder = rule.evaluate(perennial(RAINY_DAY)
                    .status(PlantingStatus.GROWING)
                    .planted(RAINY_DAY.minusMonths(6))
                    .build());

            assertThat(reminder).isPresent();
            assertThat(reminder.get().severity()).isEqualTo(ReminderSeverity.OVERDUE);
            assertThat(reminder.get().detail()).contains("6 tháng tuổi");
        }

        @Test
        void anyRecentEntryCountsAsTendingTheOrchard() {
            CareContext context = perennial(RAINY_DAY)
                    .status(PlantingStatus.GROWING)
                    .planted(RAINY_DAY.minusMonths(6))
                    .did(ActivityType.WEEDING, RAINY_DAY.minusDays(10))
                    .build();

            assertThat(rule.evaluate(context)).isEmpty();
        }

        @Test
        void doesNotApplyOnceTheOrchardIsProducing() {
            CareContext context = perennial(RAINY_DAY)
                    .status(PlantingStatus.PRODUCING)
                    .planted(RAINY_DAY.minusMonths(6))
                    .build();

            assertThat(rule.evaluate(context)).isEmpty();
        }

        @Test
        void doesNotApplyToAnOrchardOlderThanThreeYears() {
            CareContext context = perennial(RAINY_DAY)
                    .status(PlantingStatus.GROWING)
                    .planted(RAINY_DAY.minusMonths(40))
                    .build();

            assertThat(rule.evaluate(context)).isEmpty();
        }
    }

    private static Builder perennial(LocalDate today) {
        return new Builder(today, true);
    }

    private static Builder annual(LocalDate today) {
        return new Builder(today, false);
    }

    private static final class Builder {

        private final LocalDate today;
        private final boolean perennial;
        private final java.util.Map<ActivityType, LocalDate> lastActivity = new java.util.EnumMap<>(ActivityType.class);
        private LocalDate plantingDate;
        private PlantingStatus status = PlantingStatus.PRODUCING;
        private LocalDate lastHarvest;

        private Builder(LocalDate today, boolean perennial) {
            this.today = today;
            this.perennial = perennial;
            this.plantingDate = today.minusYears(8);
        }

        Builder planted(LocalDate date) {
            this.plantingDate = date;
            return this;
        }

        Builder status(PlantingStatus status) {
            this.status = status;
            return this;
        }

        Builder did(ActivityType type, LocalDate date) {
            lastActivity.put(type, date);
            return this;
        }

        Builder watered(LocalDate date) {
            return did(ActivityType.WATERING, date);
        }

        Builder harvested(LocalDate date) {
            this.lastHarvest = date;
            return this;
        }

        CareContext build() {
            int age = (int) Period.between(plantingDate, today).toTotalMonths();
            return new CareContext(1L, "Cà phê (Robusta)", "Lô A2", perennial, plantingDate, status, age, 1100,
                    Map.copyOf(lastActivity), lastHarvest, today);
        }
    }
}
