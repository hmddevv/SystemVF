package com.hmdao.farm.cultivation.application.dto;

import java.time.LocalDate;

public record CorrectPlantingCommand(LocalDate plantingDate, int treeCount) {
}
