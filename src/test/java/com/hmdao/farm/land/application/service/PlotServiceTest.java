package com.hmdao.farm.land.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import com.hmdao.farm.land.application.dto.PlotCommand;
import com.hmdao.farm.land.application.port.out.FarmRepository;
import com.hmdao.farm.land.application.port.out.PlotRepository;
import com.hmdao.farm.land.application.port.out.PlotUsagePort;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Service được test hoàn toàn qua mock của output port — không Spring, không DB.
 * Đây là lợi ích trực tiếp của Dependency Inversion.
 */
class PlotServiceTest {

    private static final Long OWNER = 1L;

    private final PlotRepository plots = mock(PlotRepository.class);
    private final FarmRepository farms = mock(FarmRepository.class);
    private final CurrentUserProvider currentUser = () -> OWNER;
    private final PlotUsagePort usagePort = mock(PlotUsagePort.class);
    private final PlotService service = new PlotService(plots, farms, currentUser, List.of(usagePort));

    private final Farm farm = Farm.create(OWNER, "Nông trại Cư M'gar", null);

    @BeforeEach
    void setUp() {
        when(plots.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsPlotInOwnedFarm() {
        when(farms.findByIdAndOwnerId(10L, OWNER)).thenReturn(Optional.of(farm));

        var view = service.create(10L, new PlotCommand("Lô A2", 15_000, "Đất đỏ bazan"));

        assertThat(view.name()).isEqualTo("Lô A2");
        assertThat(view.areaHectares()).isEqualTo(1.5);
    }

    @Test
    void br11_cannotAddPlotToFarmOfAnotherOwner() {
        when(farms.findByIdAndOwnerId(10L, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(10L, new PlotCommand("Lô A2", 100, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(plots, never()).save(any());
    }

    @Test
    void br01_rejectsDuplicatePlotNameInSameFarm() {
        when(farms.findByIdAndOwnerId(10L, OWNER)).thenReturn(Optional.of(farm));
        when(plots.existsByFarmIdAndNameIgnoreCase(10L, "Lô A2")).thenReturn(true);

        assertThatThrownBy(() -> service.create(10L, new PlotCommand(" Lô A2 ", 100, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-01");
        verify(plots, never()).save(any());
    }

    @Test
    void br10_cannotDeletePlotThatStillHasPlantings() {
        Plot plot = Plot.create(farm, "Lô A2", 15_000, null);
        when(plots.findByIdAndFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(plot));
        when(usagePort.describeUsage(5L)).thenReturn(Optional.of("2 lứa trồng"));

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("2 lứa trồng");
        verify(plots, never()).delete(any());
    }

    @Test
    void deletesPlotNotUsedByAnyModule() {
        Plot plot = Plot.create(farm, "Lô A2", 15_000, null);
        when(plots.findByIdAndFarmOwnerId(5L, OWNER)).thenReturn(Optional.of(plot));
        when(usagePort.describeUsage(5L)).thenReturn(Optional.empty());

        service.delete(5L);

        verify(plots).delete(plot);
    }
}
