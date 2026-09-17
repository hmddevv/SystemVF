package com.hmdao.farm.land.web.dto;

import com.hmdao.farm.land.application.dto.PlotCommand;
import com.hmdao.farm.land.domain.Plot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PlotRequest(
        @Schema(example = "Lô A2")
        @NotBlank(message = "Tên lô đất không được để trống")
        @Size(max = Plot.NAME_MAX, message = "Tên lô đất tối đa {max} ký tự")
        String name,

        @Schema(description = "Diện tích (m²)", example = "15000")
        @NotNull(message = "Diện tích không được để trống")
        @Positive(message = "Diện tích phải lớn hơn 0 m²")
        @DecimalMax(value = "100000000", message = "Diện tích tối đa 100.000.000 m² (10.000 ha) — kiểm tra lại đơn vị")
        Double areaM2,

        @Schema(example = "Đất đỏ bazan")
        @Size(max = Plot.SOIL_TYPE_MAX, message = "Loại đất tối đa {max} ký tự")
        String soilType) {

    public PlotCommand toCommand() {
        return new PlotCommand(name, areaM2, soilType);
    }
}
