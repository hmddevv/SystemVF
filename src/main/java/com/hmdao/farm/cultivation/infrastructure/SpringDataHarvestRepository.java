package com.hmdao.farm.cultivation.infrastructure;

import com.hmdao.farm.cultivation.application.dto.HarvestTotals;
import com.hmdao.farm.cultivation.application.port.out.HarvestRepository;
import com.hmdao.farm.cultivation.domain.Harvest;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Xem {@link SpringDataActivityRepository} về cách chuyển đổi kiểu phân trang. */
interface SpringDataHarvestRepository extends Repository<Harvest, Long>, HarvestRepository {

    @Override
    @EntityGraph(attributePaths = {"season", "season.planting", "season.planting.crop"})
    Optional<Harvest> findByIdAndSeasonPlantingPlotFarmOwnerId(Long id, Long ownerId);

    @Override
    default Page<Harvest> findPageBySeasonId(Long seasonId, PageRequest request) {
        var page = findAllBySeasonIdOrderByHarvestDateDescIdDesc(seasonId,
                org.springframework.data.domain.PageRequest.of(request.page(), request.size()));
        return new Page<>(page.getContent(), request.page(), request.size(), page.getTotalElements());
    }

    @EntityGraph(attributePaths = "season")
    org.springframework.data.domain.Page<Harvest> findAllBySeasonIdOrderByHarvestDateDescIdDesc(
            Long seasonId, Pageable pageable);

    @Override
    @Query("""
            select new com.hmdao.farm.cultivation.application.dto.HarvestTotals(
                h.season.id, count(h), sum(h.quantityKg), sum(h.revenue))
            from Harvest h
            where h.season.id in :seasonIds
            group by h.season.id
            """)
    List<HarvestTotals> totalsBySeasonIds(@Param("seasonIds") Collection<Long> seasonIds);
}
