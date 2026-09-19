package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.HarvestView;
import com.hmdao.farm.cultivation.application.dto.RecordHarvestCommand;

/** Epic E — thu hoạch. Lần thu hoạch đầu tiên đưa lứa đang GROWING sang PRODUCING (BR-09). */
public interface RecordHarvestUseCase {

    HarvestView record(Long plantingId, RecordHarvestCommand command);

    HarvestView correct(Long harvestId, RecordHarvestCommand command);

    void delete(Long harvestId);
}
