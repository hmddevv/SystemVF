package com.hmdao.farm.land.application.port.out;

import com.hmdao.farm.land.application.dto.FarmLandSummary;
import com.hmdao.farm.land.domain.Plot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Output port lưu trữ lô đất. Quyền sở hữu được kiểm tra qua nông trại chứa lô (BR-11).
 */
public interface PlotRepository {

    Plot save(Plot plot);

    Optional<Plot> findByIdAndFarmOwnerId(Long id, Long ownerId);

    List<Plot> findAllByFarmIdOrderByNameAsc(Long farmId);

    boolean existsByFarmIdAndNameIgnoreCase(Long farmId, String name);

    boolean existsByFarmIdAndNameIgnoreCaseAndIdNot(Long farmId, String name, Long excludedPlotId);

    long countByFarmId(Long farmId);

    /** Một truy vấn GROUP BY cho tất cả nông trại cần hiển thị. Nông trại không có lô sẽ vắng mặt. */
    List<FarmLandSummary> summarizeByFarmIds(Collection<Long> farmIds);

    void delete(Plot plot);
}
