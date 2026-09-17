package com.hmdao.farm.catalog.application.port.out;

import com.hmdao.farm.catalog.domain.Crop;
import java.util.List;
import java.util.Optional;

public interface CropRepository {

    Crop save(Crop crop);

    Optional<Crop> findById(Long id);

    List<Crop> findAllByOrderByNameAscVarietyAsc();

    /** Trùng tên + giống, không phân biệt hoa thường; giống rỗng coi như bằng nhau. */
    boolean existsDuplicate(String name, String variety);

    boolean existsDuplicateExcluding(String name, String variety, Long excludedCropId);

    void delete(Crop crop);
}
