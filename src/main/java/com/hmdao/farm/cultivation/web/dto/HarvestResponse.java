package com.hmdao.farm.cultivation.web.dto;

import com.hmdao.farm.cultivation.application.dto.HarvestView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HarvestResponse(
        @Schema(example = "12") Long id,
        @Schema(description = "Niên vụ hệ thống tự gán theo ngày thu hoạch", example = "7") Long seasonId,
        @Schema(example = "2025/2026") String seasonLabel,
        @Schema(example = "2025-11-28") LocalDate harvestDate,
        @Schema(example = "3200.5") double quantityKg,
        @Schema(example = "76800000.00") BigDecimal revenue,
        @Schema(description = "Doanh thu chia sản lượng — giá bán thực tế của lần thu hoạch này",
                example = "24000.00") BigDecimal pricePerKg) {

    public static HarvestResponse from(HarvestView view) {
        return new HarvestResponse(view.id(), view.seasonId(), view.seasonLabel(), view.harvestDate(),
                view.quantityKg(), view.revenue(), view.pricePerKg());
    }
}
