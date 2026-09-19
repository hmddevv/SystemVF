package com.hmdao.farm.cultivation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ActivityTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    private final Plot plot = Plot.create(Farm.create(1L, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    private final Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);
    private final Planting planting = Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true, TODAY);

    private Season seasonOf(LocalDate date) {
        return Season.open(planting, SeasonPolicy.windowContaining(planting, date));
    }

    @Test
    void logsCostRoundedToTwoDecimals() {
        Season season = seasonOf(LocalDate.of(2025, 6, 10));

        Activity activity = Activity.log(season, ActivityType.FERTILIZING, LocalDate.of(2025, 6, 10),
                new BigDecimal("12500000.456"), "  Bón NPK  ");

        assertThat(activity.getCost()).isEqualByComparingTo("12500000.46");
        assertThat(activity.getNote()).isEqualTo("Bón NPK");
    }

    @Test
    void br08_missingCostMeansZero() {
        Season season = seasonOf(LocalDate.of(2025, 6, 10));

        Activity activity = Activity.log(season, ActivityType.WEEDING, LocalDate.of(2025, 6, 10), null, null);

        assertThat(activity.getCost()).isEqualByComparingTo("0");
        assertThat(activity.getNote()).isNull();
    }

    @Test
    void br08_negativeCostIsRejected() {
        Season season = seasonOf(LocalDate.of(2025, 6, 10));

        assertThatThrownBy(() -> Activity.log(season, ActivityType.WATERING, LocalDate.of(2025, 6, 10),
                new BigDecimal("-1"), null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-08");
    }

    @Test
    void br12_otherActivityWithoutNoteIsRejected() {
        Season season = seasonOf(LocalDate.of(2025, 6, 10));

        assertThatThrownBy(() -> Activity.log(season, ActivityType.OTHER, LocalDate.of(2025, 6, 10),
                BigDecimal.TEN, "   "))
                .hasFieldOrPropertyWithValue("ruleCode", "BR-12");

        assertThat(Activity.log(season, ActivityType.OTHER, LocalDate.of(2025, 6, 10), BigDecimal.TEN,
                "Thuê máy xới đất").getNote()).isEqualTo("Thuê máy xới đất");
    }

    @Test
    void br07_dateMustFallInsideItsSeason() {
        Season season = seasonOf(LocalDate.of(2025, 6, 10));

        // 1/2/2026 đã sang niên vụ 2026, không thuộc cửa sổ 1/2/2025 – 31/1/2026
        assertThatThrownBy(() -> Activity.log(season, ActivityType.WATERING, LocalDate.of(2026, 2, 1),
                BigDecimal.ZERO, null))
                .hasFieldOrPropertyWithValue("ruleCode", "BR-07")
                .hasMessageContaining("2025/2026");
    }

    @Test
    void correctionMovesTheEntryToTheSeasonMatchingTheNewDate() {
        Season oldSeason = seasonOf(LocalDate.of(2025, 6, 10));
        Activity activity = Activity.log(oldSeason, ActivityType.SPRAYING, LocalDate.of(2025, 6, 10),
                new BigDecimal("500000"), null);
        Season newSeason = seasonOf(LocalDate.of(2026, 3, 5));

        activity.correct(newSeason, ActivityType.SPRAYING, LocalDate.of(2026, 3, 5), new BigDecimal("500000"), null);

        assertThat(activity.getSeason()).isSameAs(newSeason);
        assertThat(activity.getSeason().getYear()).isEqualTo(2026);
    }
}
