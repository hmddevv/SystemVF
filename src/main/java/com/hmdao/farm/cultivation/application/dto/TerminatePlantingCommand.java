package com.hmdao.farm.cultivation.application.dto;

import com.hmdao.farm.cultivation.domain.EndReason;
import java.time.LocalDate;

public record TerminatePlantingCommand(LocalDate endDate, EndReason reason, String note) {
}
