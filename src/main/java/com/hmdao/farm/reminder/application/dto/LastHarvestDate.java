package com.hmdao.farm.reminder.application.dto;

import java.time.LocalDate;

public record LastHarvestDate(Long plantingId, LocalDate lastDate) {
}
