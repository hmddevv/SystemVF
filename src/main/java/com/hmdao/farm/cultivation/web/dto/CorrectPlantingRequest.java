package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.CorrectPlantingCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CorrectPlantingRequest(
        @Schema(example = "2016-06-15")
        @NotNull(message = "Ngày trồng không được để trống")
        LocalDate plantingDate,

        @Schema(example = "1080")
        @NotNull(message = "Số cây không được để trống")
        @Positive(message = "Số cây phải lớn hơn 0")
        @Max(value = 10_000_000, message = "Số cây tối đa {value} — kiểm tra lại số liệu")
        Integer treeCount) {

    public CorrectPlantingCommand toCommand() {
        return new CorrectPlantingCommand(plantingDate, treeCount);
    }
}
