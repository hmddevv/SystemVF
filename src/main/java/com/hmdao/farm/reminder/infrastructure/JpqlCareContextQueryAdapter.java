package com.hmdao.farm.reminder.infrastructure;

import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.reminder.application.dto.LastActivityDate;
import com.hmdao.farm.reminder.application.dto.LastHarvestDate;
import com.hmdao.farm.reminder.application.dto.PlantingSnapshot;
import com.hmdao.farm.reminder.application.port.out.CareContextQueryPort;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** JPQL aggregate + DTO projection; không nạp entity nào vào persistence context. */
interface JpqlCareContextQueryAdapter extends Repository<Planting, Long>, CareContextQueryPort {

    @Override
    @Query("""
            select new com.hmdao.farm.reminder.application.dto.PlantingSnapshot(
                p.id, c.name, c.variety, pl.name, c.perennial, p.plantingDate, p.status, p.treeCount)
            from Planting p
                join p.crop c
                join p.plot pl
                join pl.farm f
            where f.ownerId = :ownerId
                and (:farmId is null or f.id = :farmId)
                and p.status <> com.hmdao.farm.cultivation.domain.PlantingStatus.TERMINATED
            """)
    List<PlantingSnapshot> findActivePlantings(@Param("ownerId") Long ownerId, @Param("farmId") Long farmId);

    @Override
    @Query("select count(f) > 0 from Farm f where f.id = :farmId and f.ownerId = :ownerId")
    boolean farmBelongsToOwner(@Param("ownerId") Long ownerId, @Param("farmId") Long farmId);

    @Override
    @Query("""
            select new com.hmdao.farm.reminder.application.dto.LastActivityDate(
                s.planting.id, a.type, max(a.activityDate))
            from Activity a
                join a.season s
            where s.planting.id in :plantingIds
            group by s.planting.id, a.type
            """)
    List<LastActivityDate> lastActivityDates(@Param("plantingIds") Collection<Long> plantingIds);

    @Override
    @Query("""
            select new com.hmdao.farm.reminder.application.dto.LastHarvestDate(
                s.planting.id, max(h.harvestDate))
            from Harvest h
                join h.season s
            where s.planting.id in :plantingIds
            group by s.planting.id
            """)
    List<LastHarvestDate> lastHarvestDates(@Param("plantingIds") Collection<Long> plantingIds);
}
