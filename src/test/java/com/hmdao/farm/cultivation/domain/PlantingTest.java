package com.hmdao.farm.cultivation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlantingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    private final Plot plot = Plot.create(Farm.create(1L, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    private final Crop pepper = Crop.create("Hồ tiêu", "Vĩnh Linh", true, 5);

    private Planting pepperPlanted(boolean producing) {
        return Planting.plant(plot, pepper, LocalDate.of(2019, 7, 1), 400, producing, TODAY);
    }

    @Nested
    class Planting_ {

        @Test
        void newPlantingIsGrowingByDefault() {
            assertThat(pepperPlanted(false).getStatus()).isEqualTo(PlantingStatus.GROWING);
        }

        @Test
        void digitizingAnExistingOrchardCanStartAsProducing() {
            assertThat(pepperPlanted(true).getStatus()).isEqualTo(PlantingStatus.PRODUCING);
        }

        @Test
        void br02_plantingDateCannotBeInTheFuture() {
            assertThatThrownBy(() -> Planting.plant(plot, pepper, TODAY.plusDays(1), 10, false, TODAY))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-02");
        }

        @Test
        void plantingTodayIsAllowed() {
            assertThat(Planting.plant(plot, pepper, TODAY, 10, false, TODAY).getPlantingDate()).isEqualTo(TODAY);
        }

        @Test
        void br02_treeCountMustBePositive() {
            assertThatThrownBy(() -> Planting.plant(plot, pepper, TODAY, 0, false, TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-02");
        }
    }

    @Nested
    class Lifecycle {

        @ParameterizedTest(name = "{0} -> {1} = {2}")
        @CsvSource({
                "GROWING, PRODUCING, true",
                "GROWING, TERMINATED, true",
                "PRODUCING, TERMINATED, true",
                "PRODUCING, GROWING, false",
                "PRODUCING, PRODUCING, false",
                "TERMINATED, GROWING, false",
                "TERMINATED, PRODUCING, false",
                "TERMINATED, TERMINATED, false"
        })
        void br03_transitionTable(PlantingStatus from, PlantingStatus to, boolean allowed) {
            assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        }

        @Test
        void br03_producingPlantingCannotStartProducingAgain() {
            Planting planting = pepperPlanted(true);

            assertThatThrownBy(planting::startProducing).hasFieldOrPropertyWithValue("ruleCode", "BR-03");
        }

        @Test
        void terminateRecordsDateReasonAndNote() {
            Planting planting = pepperPlanted(true);

            planting.terminate(LocalDate.of(2026, 8, 20), EndReason.PEST_DISEASE, " Chết nhanh ", TODAY);

            assertThat(planting.getStatus()).isEqualTo(PlantingStatus.TERMINATED);
            assertThat(planting.isActive()).isFalse();
            assertThat(planting.getEndReason()).isEqualTo(EndReason.PEST_DISEASE);
            assertThat(planting.getEndNote()).isEqualTo("Chết nhanh");
        }

        @Test
        void br03_terminatedIsFinal() {
            Planting planting = pepperPlanted(true);
            planting.terminate(LocalDate.of(2026, 8, 20), EndReason.PEST_DISEASE, null, TODAY);

            assertThatThrownBy(() -> planting.terminate(TODAY, EndReason.OTHER, null, TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-03")
                    .hasMessageContaining("2026-08-20");
            assertThatThrownBy(planting::startProducing).hasFieldOrPropertyWithValue("ruleCode", "BR-03");
        }

        @Test
        void br04_reasonIsRequired() {
            Planting planting = pepperPlanted(true);

            assertThatThrownBy(() -> planting.terminate(TODAY, null, null, TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-04");
            assertThat(planting.isActive()).isTrue();
        }

        @Test
        void br04_endDateMustBeBetweenPlantingDateAndToday() {
            Planting planting = pepperPlanted(true);

            assertThatThrownBy(() -> planting.terminate(LocalDate.of(2019, 6, 30), EndReason.MARKET, null, TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-04");
            assertThatThrownBy(() -> planting.terminate(TODAY.plusDays(1), EndReason.MARKET, null, TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-04");
            assertThat(planting.getStatus()).isEqualTo(PlantingStatus.PRODUCING);
        }
    }

    @Nested
    class Recording {

        @Test
        void br07_dateBeforePlantingIsRejected() {
            Planting planting = pepperPlanted(true);

            assertThatThrownBy(() -> planting.requireRecordable(LocalDate.of(2019, 6, 30), TODAY))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-07");
        }

        @Test
        void br07_futureDateIsRejected() {
            Planting planting = pepperPlanted(true);

            assertThatThrownBy(() -> planting.requireRecordable(TODAY.plusDays(1), TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-07");
        }

        @Test
        void br07_nothingIsRecordedAfterTermination() {
            Planting planting = pepperPlanted(true);
            planting.terminate(LocalDate.of(2026, 8, 20), EndReason.PEST_DISEASE, null, TODAY);

            assertThatThrownBy(() -> planting.requireRecordable(LocalDate.of(2026, 8, 21), TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-07")
                    .hasMessageContaining("2026-08-20");
        }

        @Test
        void terminatedPlantingStillAcceptsEntriesFromWhenItWasAlive() {
            Planting planting = pepperPlanted(true);
            planting.terminate(LocalDate.of(2026, 8, 20), EndReason.PEST_DISEASE, null, TODAY);

            // Nhà nông thường nhập liệu muộn hơn thực tế — chặn hẳn sẽ mất dữ liệu có thật.
            planting.requireRecordable(LocalDate.of(2026, 8, 20), TODAY);
        }
    }

    @Nested
    class Correction {

        @Test
        void br04_cannotMovePlantingDateAfterEndDate() {
            Planting planting = pepperPlanted(true);
            planting.terminate(LocalDate.of(2026, 8, 20), EndReason.WEATHER, null, TODAY);

            assertThatThrownBy(() -> planting.correct(LocalDate.of(2026, 9, 1), 400, TODAY))
                    .hasFieldOrPropertyWithValue("ruleCode", "BR-04");
        }

        @Test
        void ageIsFrozenAtEndDate() {
            Planting planting = pepperPlanted(true);
            planting.terminate(LocalDate.of(2025, 7, 1), EndReason.OLD_AGE, null, TODAY);

            assertThat(planting.ageInMonths(TODAY)).isEqualTo(72);
        }
    }
}
