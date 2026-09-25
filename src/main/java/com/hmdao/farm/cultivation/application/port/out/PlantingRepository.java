package com.hmdao.farm.cultivation.application.port.out;

import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import java.util.List;
import java.util.Optional;

public interface PlantingRepository {

    Planting save(Planting planting);

    /** Lọc theo chủ sở hữu qua lô → nông trại (BR-11); nạp sẵn lô và cây trồng. */
    Optional<Planting> findByIdAndPlotFarmOwnerId(Long id, Long ownerId);

    /** Toàn bộ lịch sử của lô, mới nhất trước; nạp sẵn cây trồng trong cùng truy vấn. */
    List<Planting> findAllByPlotIdOrderByPlantingDateDescIdDesc(Long plotId);

    List<Planting> findAllByPlotIdAndStatusNotOrderByPlantingDateDescIdDesc(Long plotId, PlantingStatus excluded);

    /**
     * Lứa trồng thuộc chủ sở hữu, lọc tuỳ chọn theo nông trại; nạp sẵn lô và cây trồng trong cùng
     * một truy vấn. Xếp theo tên lô, rồi lứa mới trồng trước.
     */
    List<Planting> findAllOwned(Long ownerId, Long farmId, boolean activeOnly);

    long countByPlotId(Long plotId);

    long countByPlotIdAndStatusNot(Long plotId, PlantingStatus excluded);

    long countByCropId(Long cropId);

    void delete(Planting planting);
}
