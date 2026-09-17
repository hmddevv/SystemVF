package com.hmdao.farm.cultivation.application.dto;

import java.time.LocalDate;

public record PlantCropCommand(Long cropId, LocalDate plantingDate, int treeCount, boolean alreadyProducing) {
}
