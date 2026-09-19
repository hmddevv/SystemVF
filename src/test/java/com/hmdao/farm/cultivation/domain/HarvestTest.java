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

class HarvestTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    private final Plot plot = Plot.create(Farm.create(1L, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    private final Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);
    private final Planting planting = Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true, TODAY);
    private final Season season = Season.open(planting,
            new PerennialSeasonPolicy().windowContaining(planting, LocalDate.of(2025, 11, 28)));

    @Test
    void recordsQuantityAndRevenue() {
        Harvest harvest = Harvest.record(season, LocalDate.of(2025, 11, 28), 3200.5, new BigDecimal("76800000"));

        assertThat(harvest.getQuantityKg()).isEqualTo(3200.5);
        assertThat(harvest.getRevenue()).isEqualByComparingTo("76800000.00");
        assertThat(harvest.getSeason().getLabel()).isEqualTo("2025/2026");
    }

    @Test
    void br08_quantityMustBePositive() {
        assertThatThrownBy(() -> Harvest.record(season, LocalDate.of(2025, 11, 28), 0, BigDecimal.TEN))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-08");
    }

    @Test
    void br08_revenueMayBeMissingWhenHarvestedButNotSoldYet() {
        Harvest harvest = Harvest.record(season, LocalDate.of(2025, 11, 28), 500, null);

        assertThat(harvest.getRevenue()).isEqualByComparingTo("0");
    }

    @Test
    void br08_negativeRevenueIsRejected() {
        assertThatThrownBy(() -> Harvest.record(season, LocalDate.of(2025, 11, 28), 500, new BigDecimal("-1")))
                .hasFieldOrPropertyWithValue("ruleCode", "BR-08");
    }

    @Test
    void br07_dateMustFallInsideItsSeason() {
        assertThatThrownBy(() -> Harvest.record(season, LocalDate.of(2026, 2, 1), 500, BigDecimal.TEN))
                .hasFieldOrPropertyWithValue("ruleCode", "BR-07");
    }
}
