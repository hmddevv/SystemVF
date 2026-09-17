package com.hmdao.farm.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CropTest {

    @Test
    void perennialCropKeepsSeasonStartMonth() {
        Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);

        assertThat(coffee.getSeasonStart()).isEqualTo(Month.FEBRUARY);
        assertThat(coffee.getDisplayName()).isEqualTo("Cà phê (Robusta)");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 13})
    void br05a_perennialCropRequiresValidSeasonStartMonth(Integer month) {
        assertThatThrownBy(() -> Crop.create("Hồ tiêu", "Vĩnh Linh", true, month))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-05a");
    }

    @Test
    void br05a_annualCropMustNotHaveSeasonStartMonth() {
        assertThatThrownBy(() -> Crop.create("Ngô", "LVN10", false, 3))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-05a");
    }

    @Test
    void annualCropWithoutVarietyUsesNameAsDisplayName() {
        Crop corn = Crop.create("Ngô", " ", false, null);

        assertThat(corn.getVariety()).isNull();
        assertThat(corn.getDisplayName()).isEqualTo("Ngô");
        assertThat(corn.getSeasonStart()).isNull();
    }
}
