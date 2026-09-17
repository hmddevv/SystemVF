package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.PlantCropCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record PlantCropRequest(
        @Schema(description = "Id cây trồng trong danh mục", example = "1")
        @NotNull(message = "Phải chọn cây trồng")
        Long cropId,

        @Schema(description = "Ngày trồng, không ở tương lai", example = "2016-06-15")
        @NotNull(message = "Ngày trồng không được để trống")
        LocalDate plantingDate,

        @Schema(example = "1100")
        @NotNull(message = "Số cây không được để trống")
        @Positive(message = "Số cây phải lớn hơn 0")
        @Max(value = 10_000_000, message = "Số cây tối đa {value} — kiểm tra lại số liệu")
        Integer treeCount,

        @Schema(description = "true nếu vườn đã cho thu hoạch (số hóa vườn có sẵn). Mặc định false.",
                example = "true")
        Boolean alreadyProducing) {

    public PlantCropCommand toCommand() {
        return new PlantCropCommand(cropId, plantingDate, treeCount, Boolean.TRUE.equals(alreadyProducing));
    }
}
