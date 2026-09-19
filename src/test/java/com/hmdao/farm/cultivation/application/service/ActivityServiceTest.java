package com.hmdao.farm.cultivation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.cultivation.application.dto.ActivityView;
import com.hmdao.farm.cultivation.application.dto.LogActivityCommand;
import com.hmdao.farm.cultivation.application.port.out.ActivityRepository;
import com.hmdao.farm.cultivation.application.port.out.HarvestRepository;
import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Activity;
import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.cultivation.domain.AnnualSeasonPolicy;
import com.hmdao.farm.cultivation.domain.PerennialSeasonPolicy;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ActivityServiceTest {

    private static final Long OWNER = 1L;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 17/9/2026 00:30 giờ Việt Nam = 16/9/2026 17:30 UTC — "hôm nay" phải theo múi giờ nghiệp vụ. */
    private final Clock clock =
            Clock.fixed(LocalDate.of(2026, 9, 17).atTime(0, 30).atZone(VIETNAM).toInstant(), VIETNAM);

    private final PlantingRepository plantings = mock(PlantingRepository.class);
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final HarvestRepository harvests = mock(HarvestRepository.class);
    private final CultivationAccess access =
            new CultivationAccess(plantings, seasons, activities, harvests, () -> OWNER);
    private final ActivityService service =
            new ActivityService(activities, new SeasonAssigner(seasons, List.of(new PerennialSeasonPolicy(), new AnnualSeasonPolicy())), access, clock);

    private final Plot plot = Plot.create(Farm.create(OWNER, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    private final Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);
    private final Planting planting =
            Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true, LocalDate.of(2026, 9, 17));

    private void ownsPlanting() {
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));
        when(activities.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(seasons.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void br05a_opensTheSeasonContainingTheEntryDate() {
        ownsPlanting();
        when(seasons.findByPlantingIdAndYear(any(), anyInt())).thenReturn(Optional.empty());

        // Tháng 12/2025 vẫn thuộc niên vụ cà phê 2025/2026 (bắt đầu tháng 2), không phải vụ 2026.
        ActivityView view = service.log(5L, new LogActivityCommand(ActivityType.FERTILIZING,
                LocalDate.of(2025, 12, 20), new BigDecimal("12500000"), null));

        assertThat(view.seasonLabel()).isEqualTo("2025/2026");
        verify(seasons).save(any());
    }

    @Test
    void br05a_reusesTheSeasonAlreadyOpenedForThatYear() {
        ownsPlanting();
        Season existing = Season.open(planting,
                new PerennialSeasonPolicy().windowContaining(planting, LocalDate.of(2025, 12, 20)));
        when(seasons.findByPlantingIdAndYear(any(), anyInt())).thenReturn(Optional.of(existing));

        service.log(5L, new LogActivityCommand(ActivityType.WATERING, LocalDate.of(2025, 12, 20), null, null));

        verify(seasons, never()).save(any());
    }

    @Test
    void br07_rejectsFutureDateInBusinessTimeZone() {
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));

        assertThatThrownBy(() -> service.log(5L, new LogActivityCommand(ActivityType.WATERING,
                LocalDate.of(2026, 9, 18), null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-07");
        verify(activities, never()).save(any());
    }

    @Test
    void correctionMovesTheEntryWhenTheNewDateBelongsToAnotherSeason() {
        Season season2025 = Season.open(planting,
                new PerennialSeasonPolicy().windowContaining(planting, LocalDate.of(2025, 12, 20)));
        Activity activity = Activity.log(season2025, ActivityType.SPRAYING, LocalDate.of(2025, 12, 20),
                new BigDecimal("500000"), null);
        when(activities.findByIdAndSeasonPlantingPlotFarmOwnerId(31L, OWNER)).thenReturn(Optional.of(activity));
        when(seasons.findByPlantingIdAndYear(any(), anyInt())).thenReturn(Optional.empty());
        when(seasons.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityView view = service.correct(31L, new LogActivityCommand(ActivityType.SPRAYING,
                LocalDate.of(2026, 3, 5), new BigDecimal("500000"), null));

        assertThat(view.seasonLabel()).isEqualTo("2026/2027");
        assertThat(activity.getSeason().getYear()).isEqualTo(2026);
    }

    @Test
    void br11_plantingOfAnotherOwnerIsNotFound() {
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.log(5L, new LogActivityCommand(ActivityType.WATERING,
                LocalDate.of(2025, 12, 20), null, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("lứa trồng");
    }
}
