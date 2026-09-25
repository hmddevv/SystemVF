package com.hmdao.farm.cultivation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hmdao.farm.catalog.application.port.out.CropRepository;
import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.cultivation.application.dto.PlantCropCommand;
import com.hmdao.farm.cultivation.application.dto.TerminatePlantingCommand;
import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.EndReason;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import com.hmdao.farm.land.application.port.out.FarmRepository;
import com.hmdao.farm.land.application.port.out.PlotRepository;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlantingServiceTest {

    private static final Long OWNER = 1L;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 17/9/2026 00:30 giờ Việt Nam = 16/9/2026 17:30 UTC — kiểm tra "hôm nay" tính theo múi giờ nghiệp vụ. */
    private final Clock clock = Clock.fixed(LocalDate.of(2026, 9, 17).atTime(0, 30).atZone(VIETNAM).toInstant(), VIETNAM);

    private final PlantingRepository plantings = mock(PlantingRepository.class);
    private final SeasonRepository seasons = mock(SeasonRepository.class);
    private final PlotRepository plots = mock(PlotRepository.class);
    private final FarmRepository farms = mock(FarmRepository.class);
    private final CropRepository crops = mock(CropRepository.class);
    private final PlantingService service =
            new PlantingService(plantings, seasons, plots, farms, crops, () -> OWNER, clock);

    private final Plot plot = Plot.create(Farm.create(OWNER, "Nông trại Cư M'gar", null), "Lô A2", 15_000, null);
    private final Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);

    @Test
    void plantsCropOnOwnedPlotUsingBusinessTimeZoneForToday() {
        when(plots.findByIdAndFarmOwnerId(1L, OWNER)).thenReturn(Optional.of(plot));
        when(crops.findById(1L)).thenReturn(Optional.of(coffee));
        when(plantings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Theo UTC hôm nay mới là 16/9 — nếu service dùng sai múi giờ, ngày 17/9 sẽ bị coi là tương lai.
        var view = service.plant(1L, new PlantCropCommand(1L, LocalDate.of(2026, 9, 17), 1100, false));

        assertThat(view.cropName()).isEqualTo("Cà phê (Robusta)");
        assertThat(view.status()).isEqualTo(PlantingStatus.GROWING);
        assertThat(view.ageMonths()).isZero();
    }

    @Test
    void br11_cannotPlantOnPlotOfAnotherOwner() {
        when(plots.findByIdAndFarmOwnerId(1L, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.plant(1L, new PlantCropCommand(1L, LocalDate.of(2020, 1, 1), 10, false)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(plantings, never()).save(any());
    }

    @Test
    void rejectsUnknownCrop() {
        when(plots.findByIdAndFarmOwnerId(1L, OWNER)).thenReturn(Optional.of(plot));
        when(crops.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.plant(1L, new PlantCropCommand(99L, LocalDate.of(2020, 1, 1), 10, false)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("cây trồng");
    }

    @Test
    void br10_plantingWithSeasonsCannotBeDeleted() {
        Planting planting = Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true,
                LocalDate.of(2026, 9, 17));
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));
        when(seasons.countByPlantingId(5L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ResourceConflictException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-10")
                .hasMessageContaining("Cà phê (Robusta)");
        verify(plantings, never()).delete(any());
    }

    @Test
    void plantingCreatedByMistakeWithoutAnySeasonIsDeleted() {
        Planting planting = Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true,
                LocalDate.of(2026, 9, 17));
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));
        when(seasons.countByPlantingId(5L)).thenReturn(0L);

        service.delete(5L);

        verify(plantings).delete(planting);
    }

    @Test
    void terminateRejectsEndDateAfterTodayInBusinessTimeZone() {
        Planting planting = Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true, LocalDate.of(2026, 9, 17));
        when(plantings.findByIdAndPlotFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(planting));

        assertThatThrownBy(() -> service.terminate(5L,
                new TerminatePlantingCommand(LocalDate.of(2026, 9, 18), EndReason.OLD_AGE, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-04");
    }

    @Test
    void listOwnedWithoutFarmCoversEveryFarmOfTheOwner() {
        Planting planting = Planting.plant(plot, coffee, LocalDate.of(2016, 6, 15), 1100, true, LocalDate.of(2026, 9, 17));
        when(plantings.findAllOwned(OWNER, null, true)).thenReturn(List.of(planting));

        var views = service.listOwned(null, true);

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.plotName()).isEqualTo("Lô A2");
            assertThat(view.cropName()).isEqualTo("Cà phê (Robusta)");
        });
        verify(farms, never()).findByIdAndOwnerId(any(), any());
    }

    @Test
    void br11_listOwnedRejectsFarmOfAnotherOwnerAsNotFound() {
        when(farms.findByIdAndOwnerId(7L, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listOwned(7L, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nông trại");
        verify(plantings, never()).findAllOwned(any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
