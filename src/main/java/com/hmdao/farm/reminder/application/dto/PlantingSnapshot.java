package com.hmdao.farm.reminder.application.dto;

import com.hmdao.farm.cultivation.domain.PlantingStatus;
import java.time.LocalDate;

/** Lứa trồng đang canh tác trong phạm vi nhắc việc (BR-18). */
public record PlantingSnapshot(
        Long plantingId,
        String cropName,
        String cropVariety,
        String plotName,
        Boolean perennial,
        LocalDate plantingDate,
        PlantingStatus status,
        Integer treeCount) {

    public String cropDisplayName() {
        return cropVariety == null ? cropName : "%s (%s)".formatted(cropName, cropVariety);
    }
}
