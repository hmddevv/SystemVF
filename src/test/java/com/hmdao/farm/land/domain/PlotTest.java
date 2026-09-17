package com.hmdao.farm.land.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PlotTest {

    private final Farm farm = Farm.create(1L, "Nông trại Cư M'gar", null);

    @Test
    void createsPlotAndConvertsAreaToHectares() {
        Plot plot = Plot.create(farm, "  Lô A2 ", 15_000, " Đất đỏ bazan ");

        assertThat(plot.getName()).isEqualTo("Lô A2");
        assertThat(plot.getAreaHectares()).isEqualTo(1.5);
        assertThat(plot.getSoilType()).isEqualTo("Đất đỏ bazan");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void br01_rejectsNonPositiveOrInvalidArea(double area) {
        assertThatThrownBy(() -> Plot.create(farm, "Lô A2", area, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-01");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void br01_rejectsBlankName(String name) {
        assertThatThrownBy(() -> Plot.create(farm, name, 100, null))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Tên lô đất");
    }

    @Test
    void failedUpdateLeavesPlotUnchanged() {
        Plot plot = Plot.create(farm, "Lô A2", 15_000, null);

        assertThatThrownBy(() -> plot.update("Lô A3", -5, null)).isInstanceOf(BusinessRuleViolationException.class);

        assertThat(plot.getName()).isEqualTo("Lô A2");
        assertThat(plot.getAreaM2()).isEqualTo(15_000);
    }
}
