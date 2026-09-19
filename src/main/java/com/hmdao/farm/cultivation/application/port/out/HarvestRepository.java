package com.hmdao.farm.cultivation.application.port.out;

import com.hmdao.farm.cultivation.application.dto.HarvestTotals;
import com.hmdao.farm.cultivation.domain.Harvest;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HarvestRepository {

    Harvest save(Harvest harvest);

    /** Nạp sẵn niên vụ, lứa trồng và cây trồng (BR-11 đi qua cả bốn cấp). */
    Optional<Harvest> findByIdAndSeasonPlantingPlotFarmOwnerId(Long id, Long ownerId);

    Page<Harvest> findPageBySeasonId(Long seasonId, PageRequest request);

    long countBySeasonId(Long seasonId);

    /** Xem {@link ActivityRepository#totalsBySeasonIds}. */
    List<HarvestTotals> totalsBySeasonIds(Collection<Long> seasonIds);

    void delete(Harvest harvest);
}
