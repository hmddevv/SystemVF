package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.PlantCropCommand;
import com.hmdao.farm.cultivation.application.dto.PlantingView;

/** Epic B — trồng một loại cây lên lô đất; gọi nhiều lần trên cùng lô để xen canh. */
public interface PlantCropUseCase {

    PlantingView plant(Long plotId, PlantCropCommand command);
}
