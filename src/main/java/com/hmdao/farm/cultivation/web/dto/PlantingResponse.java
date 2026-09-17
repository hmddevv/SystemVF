package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.PlantingView;
import com.hmdao.farm.cultivation.domain.EndReason;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record PlantingResponse(
        @Schema(example = "2") Long id,
        @Schema(example = "1") Long plotId,
        @Schema(example = "Lô A2") String plotName,
        @Schema(example = "3") Long cropId,
        @Schema(example = "Hồ tiêu (Vĩnh Linh)") String cropName,
        @Schema(example = "true") boolean perennial,
        @Schema(example = "2019-07-01") LocalDate plantingDate,
        @Schema(example = "400") int treeCount,
        @Schema(example = "TERMINATED") PlantingStatus status,
        @Schema(description = "Tuổi vườn tính đến ngày kết thúc hoặc hôm nay", example = "85") int ageMonths,
        @Schema(description = "null khi lứa trồng chưa kết thúc", example = "2026-08-20") LocalDate endDate,
        @Schema(description = "null khi lứa trồng chưa kết thúc", example = "PEST_DISEASE") EndReason endReason,
        @Schema(description = "Ghi chú khi kết thúc, có thể null", example = "Dịch chết nhanh lan từ lô bên cạnh") String endNote) {

    public static PlantingResponse from(PlantingView view) {
        return new PlantingResponse(view.id(), view.plotId(), view.plotName(), view.cropId(), view.cropName(),
                view.perennial(), view.plantingDate(), view.treeCount(), view.status(), view.ageMonths(),
                view.endDate(), view.endReason(), view.endNote());
    }
}
