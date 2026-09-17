package com.hmdao.farm.cultivation.application.dto;

import com.hmdao.farm.cultivation.domain.EndReason;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import java.time.LocalDate;

public record PlantingView(
        Long id,
        Long plotId,
        String plotName,
        Long cropId,
        String cropName,
        boolean perennial,
        LocalDate plantingDate,
        int treeCount,
        PlantingStatus status,
        int ageMonths,
        LocalDate endDate,
        EndReason endReason,
        String endNote) {

    /** Cần {@code plot} và {@code crop} đã được nạp (xem @EntityGraph ở adapter persistence). */
    public static PlantingView of(Planting planting, LocalDate today) {
        return new PlantingView(
                planting.getId(),
                planting.getPlot().getId(),
                planting.getPlot().getName(),
                planting.getCrop().getId(),
                planting.getCrop().getDisplayName(),
                planting.getCrop().isPerennial(),
                planting.getPlantingDate(),
                planting.getTreeCount(),
                planting.getStatus(),
                planting.ageInMonths(today),
                planting.getEndDate(),
                planting.getEndReason(),
                planting.getEndNote());
    }
}
