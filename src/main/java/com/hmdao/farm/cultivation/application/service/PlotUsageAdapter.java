package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import com.hmdao.farm.land.application.port.out.PlotUsagePort;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implement port do module {@code land} khai báo: cho {@code land} biết lô đất còn lịch sử
 * canh tác mà {@code land} không phải import {@code cultivation}.
 */
@Service
@Transactional(readOnly = true)
class PlotUsageAdapter implements PlotUsagePort {

    private final PlantingRepository plantings;

    PlotUsageAdapter(PlantingRepository plantings) {
        this.plantings = plantings;
    }

    @Override
    public Optional<String> describeUsage(Long plotId) {
        long total = plantings.countByPlotId(plotId);
        if (total == 0) {
            return Optional.empty();
        }
        long active = plantings.countByPlotIdAndStatusNot(plotId, PlantingStatus.TERMINATED);
        return Optional.of("%d lứa trồng (%d đang canh tác)".formatted(total, active));
    }
}
