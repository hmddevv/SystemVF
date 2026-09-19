package com.hmdao.farm.analytics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hmdao.farm.analytics.application.dto.PlantingProfile;
import com.hmdao.farm.analytics.application.dto.ProfitLossCriteria;
import com.hmdao.farm.analytics.application.dto.ProfitLossReport;
import com.hmdao.farm.analytics.application.dto.ProfitLossRow;
import com.hmdao.farm.analytics.application.dto.SeasonCost;
import com.hmdao.farm.analytics.application.dto.SeasonYield;
import com.hmdao.farm.analytics.application.port.out.ProfitLossQueryPort;
import com.hmdao.farm.analytics.domain.ProfitLossGrouping;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Vườn mẫu: lô A2 trồng xen cà phê và hồ tiêu, lô B1 trồng cà phê, lô C mới trồng sầu riêng
 * và chưa ghi gì. Cà phê lỗ niên vụ 2024 rồi lãi niên vụ 2025 — đúng hình dáng của một vườn
 * đang ra khỏi giai đoạn kiến thiết cơ bản.
 */
class ProfitLossReportServiceTest {

    private static final Long OWNER = 1L;
    private static final long COFFEE = 1L;
    private static final long PEPPER = 3L;
    private static final long DURIAN = 4L;

    private final ProfitLossQueryPort query = mock(ProfitLossQueryPort.class);
    private final ProfitLossReportService service = new ProfitLossReportService(query, () -> OWNER);

    @BeforeEach
    void farmWithIntercroppedPlot() {
        when(query.findPlantings(eq(OWNER), any())).thenReturn(List.of(
                profile(1L, COFFEE, "Cà phê", "Robusta", 10L, "Lô A2", 15_000d, 1100),
                profile(2L, PEPPER, "Hồ tiêu", "Vĩnh Linh", 10L, "Lô A2", 15_000d, 400),
                profile(3L, COFFEE, "Cà phê", "Robusta", 11L, "Lô B1", 5_000d, 500),
                profile(4L, DURIAN, "Sầu riêng", "Ri6", 12L, "Lô C", 2_000d, 80)));
        when(query.costsByPlantingAndSeason(any())).thenReturn(List.of(
                new SeasonCost(1L, 2024, 5L, money(30_000_000)),
                new SeasonCost(1L, 2025, 7L, money(40_000_000)),
                new SeasonCost(2L, 2025, 3L, money(10_000_000)),
                new SeasonCost(3L, 2025, 2L, money(5_000_000))));
        when(query.yieldsByPlantingAndSeason(any())).thenReturn(List.of(
                new SeasonYield(1L, 2024, 2L, 1_000d, money(24_000_000)),
                new SeasonYield(1L, 2025, 3L, 3_000d, money(72_000_000)),
                new SeasonYield(2L, 2025, 1L, 500d, money(60_000_000)),
                new SeasonYield(3L, 2025, 1L, 800d, money(19_200_000))));
    }

    @Nested
    class GroupedByCrop {

        private ProfitLossReport report;

        @BeforeEach
        void runReport() {
            report = service.report(new ProfitLossCriteria(ProfitLossGrouping.CROP, null, null));
        }

        @Test
        void addsUpEverySeasonOfEveryPlantingOfThatCrop() {
            ProfitLossRow coffee = row(report, "Cà phê (Robusta)");

            assertThat(coffee.totalCost()).isEqualByComparingTo("75000000");
            assertThat(coffee.totalRevenue()).isEqualByComparingTo("115200000");
            assertThat(coffee.netProfit()).isEqualByComparingTo("40200000");
            assertThat(coffee.seasonCount()).isEqualTo(3);
            assertThat(coffee.activityCount()).isEqualTo(14);
            assertThat(coffee.harvestCount()).isEqualTo(6);
        }

        @Test
        void sortsByProfitSoTheBestAndWorstAreBothObvious() {
            assertThat(report.rows()).extracting(ProfitLossRow::label)
                    .containsExactly("Hồ tiêu (Vĩnh Linh)", "Cà phê (Robusta)", "Sầu riêng (Ri6)");
        }

        @Test
        void br14_plantingWithoutAnyEntryStillShowsUpWithZeros() {
            ProfitLossRow durian = row(report, "Sầu riêng (Ri6)");

            assertThat(durian.netProfit()).isEqualByComparingTo("0");
            assertThat(durian.seasonCount()).isZero();
            assertThat(durian.treeCount()).isEqualTo(80);
        }

        @Test
        void br15_normalisedFiguresMakeDifferentSizedPlotsComparable() {
            ProfitLossRow coffee = row(report, "Cà phê (Robusta)");

            assertThat(coffee.treeCount()).isEqualTo(1600);
            // Lô A2 (15.000) + lô B1 (5.000); lô A2 chỉ được tính một lần dù trồng xen
            assertThat(coffee.areaM2()).isEqualTo(20_000d);
            assertThat(coffee.profitPerTree()).isEqualByComparingTo("25125.00");
            assertThat(coffee.profitPer1000m2()).isEqualByComparingTo("2010000.00");
            assertThat(coffee.yieldKgPerTree()).isEqualTo(3.0);
        }

        @Test
        void br16_flagsGroupsThatShareAnIntercroppedPlot() {
            assertThat(row(report, "Cà phê (Robusta)").sharedPlot()).isTrue();
            assertThat(row(report, "Hồ tiêu (Vĩnh Linh)").sharedPlot()).isTrue();
            assertThat(row(report, "Sầu riêng (Ri6)").sharedPlot()).isFalse();
        }

        @Test
        void totalCountsEachPlotOnceEvenWhenIntercropped() {
            assertThat(report.total().netProfit()).isEqualByComparingTo("90200000");
            assertThat(report.total().areaM2()).isEqualTo(22_000d);
            assertThat(report.total().treeCount()).isEqualTo(2080);
            assertThat(report.total().label()).isEqualTo("Tổng");
        }
    }

    @Nested
    class FilteredBySeason {

        @Test
        void br13_yearSelectsSeasonsNotCalendarYears() {
            ProfitLossReport report = service.report(
                    new ProfitLossCriteria(ProfitLossGrouping.CROP, 2025, null));
            ProfitLossRow coffee = row(report, "Cà phê (Robusta)");

            assertThat(coffee.seasonCount()).isEqualTo(2);
            assertThat(coffee.netProfit()).isEqualByComparingTo("46200000");
        }

        @Test
        void br17_lifetimeFiguresIgnoreTheYearFilter() {
            ProfitLossReport report = service.report(
                    new ProfitLossCriteria(ProfitLossGrouping.CROP, 2025, null));
            ProfitLossRow coffee = row(report, "Cà phê (Robusta)");

            // Nhìn riêng vụ 2025 thì cà phê lãi 46,2 triệu; tính cả vụ 2024 lỗ thì còn 40,2 triệu
            assertThat(coffee.netProfit()).isEqualByComparingTo("46200000");
            assertThat(coffee.lifetimeNetProfit()).isEqualByComparingTo("40200000");
        }

        @Test
        void unknownYearLeavesEveryGroupAtZeroButStillVisible() {
            ProfitLossReport report = service.report(
                    new ProfitLossCriteria(ProfitLossGrouping.CROP, 1999, null));

            assertThat(report.rows()).hasSize(3);
            assertThat(report.rows()).allSatisfy(row -> assertThat(row.netProfit()).isEqualByComparingTo("0"));
        }
    }

    @Nested
    class GroupedByPlantingAndPlot {

        @Test
        void br17_paybackIsTheFirstSeasonWhereTheRunningTotalTurnsPositive() {
            ProfitLossReport report = service.report(
                    new ProfitLossCriteria(ProfitLossGrouping.PLANTING, null, null));

            // Lứa cà phê lô A2: vụ 2024 lỗ 6 triệu, vụ 2025 lãi 32 triệu -> luỹ kế dương từ vụ 2025
            ProfitLossRow coffeeA2 = row(report, "Cà phê (Robusta) — Lô A2");
            assertThat(coffeeA2.paybackYear()).isEqualTo(2025);
            assertThat(coffeeA2.lifetimeNetProfit()).isEqualByComparingTo("26000000");
        }

        @Test
        void br17_plantingWithoutDataHasNoPaybackYear() {
            ProfitLossReport report = service.report(
                    new ProfitLossCriteria(ProfitLossGrouping.PLANTING, null, null));

            assertThat(row(report, "Sầu riêng (Ri6) — Lô C").paybackYear()).isNull();
        }

        @Test
        void paybackIsOmittedWhenGroupingHidesIndividualPlantings() {
            ProfitLossReport byPlot = service.report(new ProfitLossCriteria(ProfitLossGrouping.PLOT, null, null));

            assertThat(byPlot.rows()).allSatisfy(row -> assertThat(row.paybackYear()).isNull());
            // Lô A2 gộp cà phê và hồ tiêu: 26 + 50 = 76 triệu, diện tích tính một lần
            ProfitLossRow plotA2 = row(byPlot, "Lô A2");
            assertThat(plotA2.netProfit()).isEqualByComparingTo("76000000");
            assertThat(plotA2.areaM2()).isEqualTo(15_000d);
            assertThat(plotA2.treeCount()).isEqualTo(1500);
        }
    }

    @Test
    void emptyFarmReturnsAnEmptyReportWithoutQueryingTotals() {
        when(query.findPlantings(eq(OWNER), any())).thenReturn(List.of());

        ProfitLossReport report = service.report(new ProfitLossCriteria(ProfitLossGrouping.CROP, null, null));

        assertThat(report.rows()).isEmpty();
        assertThat(report.total()).isNull();
    }

    @Test
    void br15_dividingByAMissingDenominatorGivesNullInsteadOfANonsenseNumber() {
        assertThat(ProfitLossRow.per(money(1_000), 0)).isNull();
        assertThat(ProfitLossRow.per(money(1_000), 4)).isEqualByComparingTo("250.00");
    }

    private static ProfitLossRow row(ProfitLossReport report, String label) {
        return report.rows().stream().filter(row -> row.label().equals(label)).findFirst().orElseThrow();
    }

    private static PlantingProfile profile(Long plantingId, Long cropId, String name, String variety,
            Long plotId, String plotName, Double areaM2, Integer trees) {
        return new PlantingProfile(plantingId, cropId, name, variety, plotId, plotName, areaM2, trees,
                LocalDate.of(2016, 6, 15));
    }

    private static BigDecimal money(long amount) {
        return BigDecimal.valueOf(amount);
    }
}
