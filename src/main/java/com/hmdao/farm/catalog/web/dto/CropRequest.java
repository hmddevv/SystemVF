package com.hmdao.farm.catalog.web.dto;

import com.hmdao.farm.catalog.application.dto.CropCommand;
import com.hmdao.farm.catalog.domain.Crop;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CropRequest(
        @Schema(example = "Sầu riêng")
        @NotBlank(message = "Tên cây trồng không được để trống")
        @Size(max = Crop.NAME_MAX, message = "Tên cây trồng tối đa {max} ký tự")
        String name,

        @Schema(example = "Musang King")
        @Size(max = Crop.VARIETY_MAX, message = "Tên giống tối đa {max} ký tự")
        String variety,

        @Schema(description = "true = cây lâu năm, false = cây ngắn ngày", example = "true")
        @NotNull(message = "Phải chọn cây lâu năm hay cây ngắn ngày")
        Boolean perennial,

        @Schema(description = "Tháng bắt đầu niên vụ (1–12). Bắt buộc với cây lâu năm, bỏ trống với cây ngắn ngày.",
                example = "10")
        @Min(value = 1, message = "Tháng bắt đầu niên vụ từ 1 đến 12")
        @Max(value = 12, message = "Tháng bắt đầu niên vụ từ 1 đến 12")
        Integer seasonStartMonth) {

    public CropCommand toCommand() {
        return new CropCommand(name, variety, perennial, seasonStartMonth);
    }
}
