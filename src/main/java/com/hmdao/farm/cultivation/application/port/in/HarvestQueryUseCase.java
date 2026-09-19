package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.HarvestView;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;

public interface HarvestQueryUseCase {

    HarvestView getById(Long harvestId);

    Page<HarvestView> listBySeason(Long seasonId, PageRequest request);
}
