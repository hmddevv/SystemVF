package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.catalog.application.port.out.CropUsagePort;
import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implement port do module {@code catalog} khai báo. Xem {@link PlotUsageAdapter}. */
@Service
@Transactional(readOnly = true)
class CropUsageAdapter implements CropUsagePort {

    private final PlantingRepository plantings;

    CropUsageAdapter(PlantingRepository plantings) {
        this.plantings = plantings;
    }

    @Override
    public Optional<String> describeUsage(Long cropId) {
        long total = plantings.countByCropId(cropId);
        return total == 0 ? Optional.empty() : Optional.of("%d lứa trồng".formatted(total));
    }
}
