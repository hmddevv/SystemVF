package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.LogActivityCommand;
import com.hmdao.farm.cultivation.domain.Activity;
import com.hmdao.farm.cultivation.domain.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LogActivityRequest(
        @Schema(description = "WATERING: tưới · FERTILIZING: bón phân · SPRAYING: phun thuốc · "
                + "WEEDING: làm cỏ · PRUNING: tỉa cành · OTHER: khác (bắt buộc ghi chú)",
                example = "FERTILIZING")
        @NotNull(message = "Phải chọn loại hoạt động")
        ActivityType type,

        @Schema(description = "Ngày thực hiện; quyết định niên vụ của bản ghi", example = "2025-06-10")
        @NotNull(message = "Ngày thực hiện không được để trống")
        LocalDate activityDate,

        @Schema(description = "Chi phí (VND). Bỏ trống = 0 khi tự làm, không tốn chi phí.",
                example = "12500000")
        @DecimalMin(value = "0", message = "Chi phí không được âm")
        @Digits(integer = 13, fraction = 2, message = "Chi phí tối đa {integer} chữ số phần nguyên và {fraction} chữ số thập phân")
        BigDecimal cost,

        @Schema(description = "Ghi chú; bắt buộc khi loại hoạt động là OTHER", example = "Bón NPK 16-16-8, 20 bao")
        @Size(max = Activity.NOTE_MAX, message = "Ghi chú tối đa {max} ký tự")
        String note) {

    public LogActivityCommand toCommand() {
        return new LogActivityCommand(type, activityDate, cost, note);
    }
}
