package com.hmdao.farm.land.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hmdao.farm.land.application.dto.FarmLandSummary;
import com.hmdao.farm.land.application.dto.FarmView;
import com.hmdao.farm.land.application.port.out.FarmRepository;
import com.hmdao.farm.land.application.port.out.PlotRepository;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class FarmServiceTest {

    private static final Long OWNER = 1L;

    private final FarmRepository farms = mock(FarmRepository.class);
    private final PlotRepository plots = mock(PlotRepository.class);
    private final FarmService service = new FarmService(farms, plots, () -> OWNER);

    @Test
    void br10_cannotDeleteFarmThatStillHasPlots() {
        when(farms.findByIdAndOwnerId(1L, OWNER)).thenReturn(Optional.of(Farm.create(OWNER, "Nông trại A", null)));
        when(plots.countByFarmId(1L)).thenReturn(3L);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ResourceConflictException.class)
                .hasFieldOrPropertyWithValue("ruleCode", "BR-10")
                .hasMessageContaining("3 lô đất");
        verify(farms, never()).delete(any());
    }

    @Test
    void listUsesOneSummaryQueryForAllFarmsAndDefaultsFarmsWithoutPlots() {
        Farm withPlots = withId(Farm.create(OWNER, "A", null), 1L);
        Farm empty = withId(Farm.create(OWNER, "B", null), 2L);
        when(farms.findAllByOwnerIdOrderByNameAsc(OWNER)).thenReturn(List.of(withPlots, empty));
        when(plots.summarizeByFarmIds(anyCollection())).thenReturn(List.of(new FarmLandSummary(1L, 2L, 23_000d)));

        List<FarmView> views = service.listMine();

        assertThat(views).extracting(FarmView::plotCount).containsExactly(2L, 0L);
        assertThat(views).extracting(FarmView::totalAreaM2).containsExactly(23_000d, 0d);
        verify(plots).summarizeByFarmIds(List.of(1L, 2L));
    }

    private static Farm withId(Farm farm, Long id) {
        Farm spied = spy(farm);
        when(spied.getId()).thenReturn(id);
        return spied;
    }
}
