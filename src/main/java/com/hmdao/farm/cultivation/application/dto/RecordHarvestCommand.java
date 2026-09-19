package com.hmdao.farm.cultivation.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordHarvestCommand(LocalDate harvestDate, double quantityKg, BigDecimal revenue) {
}
