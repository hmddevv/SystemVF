package com.hmdao.farm.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hmdao.farm.catalog.application.dto.CropCommand;
import com.hmdao.farm.catalog.application.port.out.CropRepository;
import com.hmdao.farm.catalog.application.port.out.CropUsagePort;
import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CropServiceTest {

    private final CropRepository crops = mock(CropRepository.class);
    private final CropUsagePort usagePort = mock(CropUsagePort.class);
    private final CropService service = new CropService(crops, List.of(usagePort));

    @Test
    void cannotSwitchPerennialFlagWhenCropAlreadyHasPlantings() {
        Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);
        when(crops.findById(1L)).thenReturn(Optional.of(coffee));
        when(usagePort.describeUsage(1L)).thenReturn(Optional.of("4 lứa trồng"));

        assertThatThrownBy(() -> service.update(1L, new CropCommand("Cà phê", "Robusta", false, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("4 lứa trồng");
        assertThat(coffee.isPerennial()).isTrue();
    }

    @Test
    void canChangeSeasonStartMonthOfCropInUse() {
        Crop coffee = Crop.create("Cà phê", "Robusta", true, 2);
        when(crops.findById(1L)).thenReturn(Optional.of(coffee));
        when(usagePort.describeUsage(1L)).thenReturn(Optional.of("4 lứa trồng"));

        var view = service.update(1L, new CropCommand("Cà phê", "Robusta", true, 3));

        assertThat(view.seasonStartMonth()).isEqualTo(3);
    }

    @Test
    void br10_cannotDeleteCropInUse() {
        when(crops.findById(1L)).thenReturn(Optional.of(Crop.create("Hồ tiêu", null, true, 5)));
        when(usagePort.describeUsage(1L)).thenReturn(Optional.of("1 lứa trồng"));

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ResourceConflictException.class);
        verify(crops, never()).delete(any());
    }
}
