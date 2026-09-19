package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.RecordHarvestCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordHarvestRequest(
        @Schema(description = "Ngày thu hoạch; quyết định niên vụ của bản ghi", example = "2025-11-28")
        @NotNull(message = "Ngày thu hoạch không được để trống")
        LocalDate harvestDate,

        @Schema(description = "Sản lượng tươi hoặc khô tùy cách nhà nông ghi, đơn vị kg", example = "3200.5")
        @NotNull(message = "Sản lượng không được để trống")
        @Positive(message = "Sản lượng phải lớn hơn 0 kg")
        @DecimalMax(value = "100000000", message = "Sản lượng tối đa {value} kg — kiểm tra lại số liệu")
        Double quantityKg,

        @Schema(description = "Doanh thu (VND). Bỏ trống = 0 khi đã thu hoạch nhưng chưa bán.",
                example = "76800000")
        @DecimalMin(value = "0", message = "Doanh thu không được âm")
        @Digits(integer = 13, fraction = 2, message = "Doanh thu tối đa {integer} chữ số phần nguyên và {fraction} chữ số thập phân")
        BigDecimal revenue) {

    public RecordHarvestCommand toCommand() {
        return new RecordHarvestCommand(harvestDate, quantityKg == null ? 0 : quantityKg, revenue);
    }
}
