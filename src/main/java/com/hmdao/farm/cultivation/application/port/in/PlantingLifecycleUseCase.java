package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.PlantingView;
import com.hmdao.farm.cultivation.application.dto.TerminatePlantingCommand;

/** Epic C — chuyển trạng thái lứa trồng. */
public interface PlantingLifecycleUseCase {

    PlantingView startProducing(Long plantingId);

    PlantingView terminate(Long plantingId, TerminatePlantingCommand command);
}
