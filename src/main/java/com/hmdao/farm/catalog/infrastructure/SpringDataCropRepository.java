package com.hmdao.farm.catalog.infrastructure;

import com.hmdao.farm.catalog.application.port.out.CropRepository;
import com.hmdao.farm.catalog.domain.Crop;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

interface SpringDataCropRepository extends Repository<Crop, Long>, CropRepository {

    String SAME_NAME_AND_VARIETY = """
            lower(c.name) = lower(:name)
            and lower(coalesce(c.variety, '')) = lower(coalesce(cast(:variety as string), ''))
            """;

    @Override
    @Query("select count(c) > 0 from Crop c where " + SAME_NAME_AND_VARIETY)
    boolean existsDuplicate(@Param("name") String name, @Param("variety") String variety);

    @Override
    @Query("select count(c) > 0 from Crop c where " + SAME_NAME_AND_VARIETY + " and c.id <> :excludedId")
    boolean existsDuplicateExcluding(@Param("name") String name, @Param("variety") String variety,
            @Param("excludedId") Long excludedCropId);
}
