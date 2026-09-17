package com.hmdao.farm.land.infrastructure;

import com.hmdao.farm.land.application.dto.FarmLandSummary;
import com.hmdao.farm.land.application.port.out.PlotRepository;
import com.hmdao.farm.land.domain.Plot;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface SpringDataPlotRepository extends Repository<Plot, Long>, PlotRepository {

    @Override
    @Query("""
            select new com.hmdao.farm.land.application.dto.FarmLandSummary(p.farm.id, count(p), sum(p.areaM2))
            from Plot p
            where p.farm.id in :farmIds
            group by p.farm.id
            """)
    List<FarmLandSummary> summarizeByFarmIds(@Param("farmIds") Collection<Long> farmIds);
}
