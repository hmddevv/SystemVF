package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.TerminatePlantingCommand;
import com.hmdao.farm.cultivation.domain.EndReason;
import com.hmdao.farm.cultivation.domain.Planting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TerminatePlantingRequest(
        @Schema(description = "Ngày cưa bỏ / chặt bỏ", example = "2026-08-20")
        @NotNull(message = "Ngày kết thúc không được để trống")
        LocalDate endDate,

        @Schema(description = "MARKET: giá thị trường · PEST_DISEASE: sâu bệnh · WEATHER: thời tiết · "
                + "OLD_AGE: già cỗi · OTHER: khác", example = "PEST_DISEASE")
        @NotNull(message = "Phải chọn lý do kết thúc")
        EndReason reason,

        @Schema(description = "Ghi chú thêm (tùy chọn)", example = "Dịch chết nhanh lan từ lô bên cạnh")
        @Size(max = Planting.END_NOTE_MAX, message = "Ghi chú tối đa {max} ký tự")
        String note) {

    public TerminatePlantingCommand toCommand() {
        return new TerminatePlantingCommand(endDate, reason, note);
    }
}
