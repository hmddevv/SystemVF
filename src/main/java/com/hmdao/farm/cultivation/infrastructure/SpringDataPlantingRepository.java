package com.hmdao.farm.cultivation.infrastructure;

import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/**
 * {@code plot} và {@code crop} là LAZY; view luôn cần tên lô và tên cây. {@code @EntityGraph}
 * nạp chúng bằng JOIN trong cùng câu SQL — danh sách N lứa trồng tốn 1 truy vấn thay vì 1 + 2N.
 */
interface SpringDataPlantingRepository extends Repository<Planting, Long>, PlantingRepository {

    @Override
    @EntityGraph(attributePaths = {"plot", "crop"})
    Optional<Planting> findByIdAndPlotFarmOwnerId(Long id, Long ownerId);

    @Override
    @EntityGraph(attributePaths = {"plot", "crop"})
    List<Planting> findAllByPlotIdOrderByPlantingDateDescIdDesc(Long plotId);

    @Override
    @EntityGraph(attributePaths = {"plot", "crop"})
    List<Planting> findAllByPlotIdAndStatusNotOrderByPlantingDateDescIdDesc(Long plotId, PlantingStatus excluded);
}
