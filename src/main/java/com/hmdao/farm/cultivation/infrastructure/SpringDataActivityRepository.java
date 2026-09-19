package com.hmdao.farm.cultivation.infrastructure;

import com.hmdao.farm.cultivation.application.dto.ActivityTotals;
import com.hmdao.farm.cultivation.application.port.out.ActivityRepository;
import com.hmdao.farm.cultivation.domain.Activity;
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

/**
 * Biên chuyển đổi giữa kiểu phân trang của ứng dụng và của Spring Data (ADR-8): method
 * {@code default} nhận {@link PageRequest}, gọi truy vấn sinh sẵn rồi trả {@link Page} — tầng
 * application không thấy {@code Pageable}.
 */
interface SpringDataActivityRepository extends Repository<Activity, Long>, ActivityRepository {

    @Override
    @EntityGraph(attributePaths = {"season", "season.planting", "season.planting.crop"})
    Optional<Activity> findByIdAndSeasonPlantingPlotFarmOwnerId(Long id, Long ownerId);

    @Override
    default Page<Activity> findPageBySeasonId(Long seasonId, PageRequest request) {
        var page = findAllBySeasonIdOrderByActivityDateDescIdDesc(seasonId,
                org.springframework.data.domain.PageRequest.of(request.page(), request.size()));
        return new Page<>(page.getContent(), request.page(), request.size(), page.getTotalElements());
    }

    @EntityGraph(attributePaths = "season")
    org.springframework.data.domain.Page<Activity> findAllBySeasonIdOrderByActivityDateDescIdDesc(
            Long seasonId, Pageable pageable);

    @Override
    @Query("""
            select new com.hmdao.farm.cultivation.application.dto.ActivityTotals(
                a.season.id, count(a), sum(a.cost))
            from Activity a
            where a.season.id in :seasonIds
            group by a.season.id
            """)
    List<ActivityTotals> totalsBySeasonIds(@Param("seasonIds") Collection<Long> seasonIds);
}
