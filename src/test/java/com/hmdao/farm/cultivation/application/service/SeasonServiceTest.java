package com.hmdao.farm.cultivation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.cultivation.application.dto.ActivityTotals;
import com.hmdao.farm.cultivation.application.dto.HarvestTotals;
import com.hmdao.farm.cultivation.application.dto.SeasonView;
import com.hmdao.farm.cultivation.application.port.out.ActivityRepository;
import com.hmdao.farm.cultivation.application.port.out.HarvestRepository;
import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.cultivation.domain.SeasonPolicy;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.BaseEntity;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SeasonServiceTest {

    private static final Long OWNER = 1L;

    private final PlantingRepository plantings = mock(PlantingRepository.class);
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final HarvestRepository harvests = mock(HarvestRepository.class);
    private final CultivationAccess access =
            new CultivationAccess(plantings, seasons, activities, harvests, () -> OWNER);
    private final SeasonService service = new SeasonService(seasons, activities, harvests, access);

    private final Plot plot = Plot.create(Farm.create(OWNER, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    private final Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);
    private final Planting planting =
            Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true, LocalDate.of(2026, 9, 17));

    private Season season(LocalDate anyDateInside, long id) {
        return withId(Season.open(planting, SeasonPolicy.windowContaining(planting, anyDateInside)), id);
    }

    @Test
    void mergesTotalsFromTheTwoGroupByQueriesAndComputesProfit() {
        Season s2025 = season(LocalDate.of(2025, 11, 28), 7L);
        Season s2024 = season(LocalDate.of(2024, 11, 28), 6L);
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));
        when(seasons.findAllByPlantingIdOrderByYearDesc(5L)).thenReturn(List.of(s2025, s2024));
        when(activities.totalsBySeasonIds(any()))
                .thenReturn(List.of(new ActivityTotals(7L, 12L, new BigDecimal("48250000.00"))));
        when(harvests.totalsBySeasonIds(any()))
                .thenReturn(List.of(new HarvestTotals(7L, 3L, 5400.5, new BigDecimal("129600000.00"))));

        List<SeasonView> views = service.listByPlanting(5L);

        SeasonView current = views.getFirst();
        assertThat(current.label()).isEqualTo("2025/2026");
        assertThat(current.activityCount()).isEqualTo(12);
        assertThat(current.totalQuantityKg()).isEqualTo(5400.5);
        assertThat(current.netProfit()).isEqualByComparingTo("81350000.00");
        assertThat(current.cropName()).isEqualTo("Cà phê (Robusta)");
    }

    @Test
    void seasonWithoutEntriesReportsZerosInsteadOfNulls() {
        Season s2024 = season(LocalDate.of(2024, 11, 28), 6L);
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));
        when(seasons.findAllByPlantingIdOrderByYearDesc(5L)).thenReturn(List.of(s2024));
        when(activities.totalsBySeasonIds(any())).thenReturn(List.of());
        when(harvests.totalsBySeasonIds(any())).thenReturn(List.of());

        SeasonView view = service.listByPlanting(5L).getFirst();

        assertThat(view.totalCost()).isEqualByComparingTo("0");
        assertThat(view.totalRevenue()).isEqualByComparingTo("0");
        assertThat(view.netProfit()).isEqualByComparingTo("0");
        assertThat(view.harvestCount()).isZero();
    }

    @Test
    void doesNotQueryTotalsWhenThePlantingHasNoSeasonYet() {
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));
        when(seasons.findAllByPlantingIdOrderByYearDesc(5L)).thenReturn(List.of());

        assertThat(service.listByPlanting(5L)).isEmpty();
        verify(activities, never()).totalsBySeasonIds(any());
        verify(harvests, never()).totalsBySeasonIds(any());
    }

    @Test
    void br10_seasonStillHoldingEntriesCannotBeDeleted() {
        Season s2025 = season(LocalDate.of(2025, 11, 28), 7L);
        when(seasons.findByIdAndPlantingPlotFarmOwnerId(7L, OWNER)).thenReturn(Optional.of(s2025));
        when(activities.countBySeasonId(7L)).thenReturn(12L);
        when(harvests.countBySeasonId(7L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(ResourceConflictException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-10")
                .hasMessageContaining("2025/2026");
        verify(seasons, never()).delete(any());
    }

    @Test
    void br10_emptySeasonCreatedByMistakeIsDeleted() {
        Season s2025 = season(LocalDate.of(2025, 11, 28), 7L);
        when(seasons.findByIdAndPlantingPlotFarmOwnerId(7L, OWNER)).thenReturn(Optional.of(s2025));
        when(activities.countBySeasonId(7L)).thenReturn(0L);
        when(harvests.countBySeasonId(7L)).thenReturn(0L);

        service.delete(7L);

        verify(seasons).delete(s2025);
    }

    /** Entity chưa qua persistence nên chưa có id; test cần id để kiểm tra việc ghép tổng hợp. */
    private static <T extends BaseEntity> T withId(T entity, long id) {
        try {
            Field field = BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
