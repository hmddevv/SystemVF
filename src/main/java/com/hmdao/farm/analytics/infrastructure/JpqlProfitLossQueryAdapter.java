package com.hmdao.farm.analytics.infrastructure;

import com.hmdao.farm.analytics.application.dto.PlantingProfile;
import com.hmdao.farm.analytics.application.dto.SeasonCost;
import com.hmdao.farm.analytics.application.dto.SeasonYield;
import com.hmdao.farm.analytics.application.port.out.ProfitLossQueryPort;
import com.hmdao.farm.cultivation.domain.Planting;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Truy vấn tổng hợp cho báo cáo: JPQL aggregate + DTO projection, không nạp entity nào vào
 * persistence context.
 *
 * <p>Chi phí và thu hoạch tách thành hai câu thay vì một câu {@code JOIN} cả hai bảng: gộp lại
 * sẽ nhân chéo dòng (12 hoạt động × 3 lần thu hoạch = 36 dòng) và cộng sai tổng.
 */
interface JpqlProfitLossQueryAdapter extends Repository<Planting, Long>, ProfitLossQueryPort {

    /**
     * Xuất phát từ {@code Planting} chứ không từ nhật ký, nên lứa trồng chưa ghi gì vẫn có mặt
     * trong báo cáo với số 0 (BR-14).
     */
    @Override
    @Query("""
            select new com.hmdao.farm.analytics.application.dto.PlantingProfile(
                p.id, c.id, c.name, c.variety, pl.id, pl.name, pl.areaM2, p.treeCount, p.plantingDate)
            from Planting p
                join p.crop c
                join p.plot pl
                join pl.farm f
            where f.ownerId = :ownerId
                and (:farmId is null or f.id = :farmId)
            order by pl.name asc, c.name asc, p.plantingDate asc
            """)
    List<PlantingProfile> findPlantings(@Param("ownerId") Long ownerId, @Param("farmId") Long farmId);

    @Override
    @Query("""
            select new com.hmdao.farm.analytics.application.dto.SeasonCost(
                s.planting.id, s.year, count(a), sum(a.cost))
            from Activity a
                join a.season s
            where s.planting.id in :plantingIds
            group by s.planting.id, s.year
            """)
    List<SeasonCost> costsByPlantingAndSeason(@Param("plantingIds") Collection<Long> plantingIds);

    @Override
    @Query("""
            select new com.hmdao.farm.analytics.application.dto.SeasonYield(
                s.planting.id, s.year, count(h), sum(h.quantityKg), sum(h.revenue))
            from Harvest h
                join h.season s
            where s.planting.id in :plantingIds
            group by s.planting.id, s.year
            """)
    List<SeasonYield> yieldsByPlantingAndSeason(@Param("plantingIds") Collection<Long> plantingIds);
}
