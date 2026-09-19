package com.hmdao.farm.cultivation.infrastructure;

import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Season;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/**
 * View niên vụ hiển thị tên cây trồng, nằm cách hai bậc LAZY (season → planting → crop).
 * {@code @EntityGraph} nạp cả chuỗi trong một câu SQL.
 */
interface SpringDataSeasonRepository extends Repository<Season, Long>, SeasonRepository {

    @Override
    @EntityGraph(attributePaths = {"planting", "planting.crop"})
    Optional<Season> findByIdAndPlantingPlotFarmOwnerId(Long id, Long ownerId);

    @Override
    @EntityGraph(attributePaths = {"planting", "planting.crop"})
    List<Season> findAllByPlantingIdOrderByYearDesc(Long plantingId);
}
