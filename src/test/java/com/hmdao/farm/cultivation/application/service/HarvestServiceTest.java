package com.hmdao.farm.cultivation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.cultivation.application.dto.HarvestView;
import com.hmdao.farm.cultivation.application.dto.RecordHarvestCommand;
import com.hmdao.farm.cultivation.application.port.out.ActivityRepository;
import com.hmdao.farm.cultivation.application.port.out.HarvestRepository;
import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Harvest;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.cultivation.domain.SeasonPolicy;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HarvestServiceTest {

    private static final Long OWNER = 1L;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock clock =
            Clock.fixed(LocalDate.of(2026, 9, 17).atTime(0, 30).atZone(VIETNAM).toInstant(), VIETNAM);

    private final PlantingRepository plantings = mock(PlantingRepository.class);
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final HarvestRepository harvests = mock(HarvestRepository.class);
    private final CultivationAccess access =
            new CultivationAccess(plantings, seasons, activities, harvests, () -> OWNER);
    private final HarvestService service =
            new HarvestService(harvests, new SeasonAssigner(seasons), access, clock);

    private final Plot plot = Plot.create(Farm.create(OWNER, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    private final Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);

    private Planting growingCoffee() {
        return Planting.plant(plot, coffee, LocalDate.of(2022, 6, 15), 1100, false, LocalDate.of(2026, 9, 17));
    }

    private void owns(Planting planting) {
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));
        when(seasons.findByPlantingIdAndYear(any(), anyInt())).thenReturn(Optional.empty());
        when(seasons.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(harvests.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void br09_firstHarvestMovesAGrowingPlantingToProducing() {
        Planting planting = growingCoffee();
        owns(planting);

        service.record(5L, new RecordHarvestCommand(LocalDate.of(2025, 11, 28), 3200.5,
                new BigDecimal("76800000")));

        assertThat(planting.getStatus()).isEqualTo(PlantingStatus.PRODUCING);
    }

    @Test
    void br09_deletingTheHarvestDoesNotSendThePlantingBackToGrowing() {
        Planting planting = growingCoffee();
        owns(planting);
        service.record(5L, new RecordHarvestCommand(LocalDate.of(2025, 11, 28), 3200.5, BigDecimal.ZERO));
        Season season = Season.open(planting, SeasonPolicy.windowContaining(planting, LocalDate.of(2025, 11, 28)));
        Harvest harvest = Harvest.record(season, LocalDate.of(2025, 11, 28), 3200.5, BigDecimal.ZERO);
        when(harvests.findByIdAndSeasonPlantingPlotFarmOwnerId(12L, OWNER)).thenReturn(Optional.of(harvest));

        service.delete(12L);

        // Lùi trạng thái tự động là hiệu ứng phụ ngầm — sửa trạng thái phải là thao tác tường minh (ADR-9).
        assertThat(planting.getStatus()).isEqualTo(PlantingStatus.PRODUCING);
    }

    @Test
    void br09_terminatedPlantingKeepsItsStatusWhenBackdatedHarvestIsRecorded() {
        Planting planting = growingCoffee();
        planting.terminate(LocalDate.of(2026, 1, 10), com.hmdao.farm.cultivation.domain.EndReason.OLD_AGE, null,
                LocalDate.of(2026, 9, 17));
        owns(planting);

        service.record(5L, new RecordHarvestCommand(LocalDate.of(2025, 11, 28), 500, BigDecimal.ZERO));

        assertThat(planting.getStatus()).isEqualTo(PlantingStatus.TERMINATED);
    }

    @Test
    void pricePerKgShowsWhatTheHarvestActuallySoldFor() {
        owns(growingCoffee());

        HarvestView view = service.record(5L, new RecordHarvestCommand(LocalDate.of(2025, 11, 28), 3200,
                new BigDecimal("76800000")));

        assertThat(view.pricePerKg()).isEqualByComparingTo("24000.00");
        assertThat(view.seasonLabel()).isEqualTo("2025/2026");
    }
}
