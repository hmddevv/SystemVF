package com.hmdao.farm.cultivation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * BR-05a — trái tim của M3: một ngày ghi nhật ký phải rơi vào đúng niên vụ, nếu không chi phí
 * chăm sóc và doanh thu thu hoạch của cùng một chu kỳ sẽ bị tách ra hai vụ khác nhau.
 */
class SeasonPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    private final Plot plot = Plot.create(Farm.create(1L, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    /** Cà phê Robusta: chu kỳ sản xuất bắt đầu tháng 2 (tưới ra hoa), thu hoạch tháng 11 đến tháng 1. */
    private final Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);
    private final Crop maize = Crop.create("Ngô", "LVN10", false, null);

    private Planting planting(Crop crop, LocalDate plantingDate) {
        return Planting.plant(plot, crop, plantingDate, 1100, true, TODAY);
    }

    @Nested
    class PerennialCrop {

        private final Planting coffeePlanting = planting(coffee, LocalDate.of(2016, 6, 15));

        @ParameterizedTest(name = "ngày {0} thuộc niên vụ {1}")
        @CsvSource({
                // Vụ thu hoạch vắt qua giao thừa vẫn là một niên vụ duy nhất
                "2025-11-28, 2025",
                "2025-12-31, 2025",
                "2026-01-15, 2025",
                "2026-01-31, 2025",
                // Sang tháng 2 là chu kỳ sản xuất mới
                "2026-02-01, 2026",
                "2025-02-01, 2025",
                "2025-01-31, 2024"
        })
        void assignsSeasonByProductionCycleNotCalendarYear(LocalDate date, int expectedYear) {
            assertThat(SeasonPolicy.windowContaining(coffeePlanting, date).year()).isEqualTo(expectedYear);
        }

        @Test
        void windowSpansOneYearStartingAtTheCropSeasonMonth() {
            SeasonWindow window = SeasonPolicy.windowContaining(coffeePlanting, LocalDate.of(2026, 1, 15));

            assertThat(window.startDate()).isEqualTo(LocalDate.of(2025, 2, 1));
            assertThat(window.endDate()).isEqualTo(LocalDate.of(2026, 1, 31));
            assertThat(window.contains(LocalDate.of(2025, 2, 1))).isTrue();
            assertThat(window.contains(LocalDate.of(2026, 1, 31))).isTrue();
            assertThat(window.contains(LocalDate.of(2026, 2, 1))).isFalse();
        }

        @Test
        void br06_firstSeasonStartsAtPlantingDateNotAtTheStartOfTheCycle() {
            // Trồng giữa vụ ngày 15/6/2016: niên vụ 2016 không thể bắt đầu từ 1/2/2016.
            SeasonWindow window = SeasonPolicy.windowContaining(coffeePlanting, LocalDate.of(2016, 8, 1));

            assertThat(window.year()).isEqualTo(2016);
            assertThat(window.startDate()).isEqualTo(LocalDate.of(2016, 6, 15));
            assertThat(window.endDate()).isEqualTo(LocalDate.of(2017, 1, 31));
        }

        @Test
        void seasonStartingLateInTheYearIsLabelledByItsStartingYear() {
            // Sầu riêng Đắk Lắk: chăm sóc lại từ tháng 10, thu hoạch tháng 7–9 năm sau.
            Crop durian = Crop.create("Sầu riêng", "Ri6", true, 10);
            Planting durianPlanting = planting(durian, LocalDate.of(2020, 5, 20));

            SeasonWindow window = SeasonPolicy.windowContaining(durianPlanting, LocalDate.of(2025, 8, 15));

            assertThat(window.year()).isEqualTo(2024);
            assertThat(window.startDate()).isEqualTo(LocalDate.of(2024, 10, 1));
            assertThat(window.endDate()).isEqualTo(LocalDate.of(2025, 9, 30));
        }
    }

    @Nested
    class AnnualCrop {

        private final Planting maizePlanting = planting(maize, LocalDate.of(2025, 4, 10));

        @Test
        void br05_hasExactlyOneOpenEndedSeason() {
            SeasonWindow first = SeasonPolicy.windowContaining(maizePlanting, LocalDate.of(2025, 4, 10));
            SeasonWindow later = SeasonPolicy.windowContaining(maizePlanting, LocalDate.of(2026, 9, 1));

            assertThat(first).isEqualTo(later);
            assertThat(first.year()).isEqualTo(2025);
            assertThat(first.startDate()).isEqualTo(LocalDate.of(2025, 4, 10));
            assertThat(first.endDate()).isNull();
        }

        @Test
        void openEndedWindowHasNoUpperBound() {
            SeasonWindow window = SeasonPolicy.windowContaining(maizePlanting, LocalDate.of(2025, 4, 10));

            assertThat(window.contains(LocalDate.of(2030, 1, 1))).isTrue();
            assertThat(window.contains(LocalDate.of(2025, 4, 9))).isFalse();
        }
    }

    @Nested
    class SeasonRecord {

        @Test
        void labelShowsBothYearsWhenTheSeasonCrossesNewYear() {
            Planting coffeePlanting = planting(coffee, LocalDate.of(2016, 6, 15));
            Season season = Season.open(coffeePlanting,
                    SeasonPolicy.windowContaining(coffeePlanting, LocalDate.of(2025, 11, 28)));

            assertThat(season.getLabel()).isEqualTo("2025/2026");
            assertThat(season.getYear()).isEqualTo(2025);
        }

        @Test
        void labelIsASingleYearForAnOpenEndedSeason() {
            Planting maizePlanting = planting(maize, LocalDate.of(2025, 4, 10));
            Season season = Season.open(maizePlanting,
                    SeasonPolicy.windowContaining(maizePlanting, LocalDate.of(2025, 6, 1)));

            assertThat(season.getLabel()).isEqualTo("2025");
            assertThat(season.getEndDate()).isNull();
        }
    }
}
