package com.hmdao.farm.analytics.application.dto;

import java.time.LocalDate;

/**
 * Hồ sơ một lứa trồng thuộc phạm vi báo cáo — nền để gom nhóm và chuẩn hoá chỉ số.
 *
 * <p>Lấy riêng khỏi phần tổng hợp tiền để lứa trồng chưa ghi gì vẫn có mặt trong báo cáo
 * với số 0 (BR-14): một câu {@code GROUP BY} trên nhật ký sẽ luôn bỏ qua nhóm rỗng.
 */
public record PlantingProfile(
        Long plantingId,
        Long cropId,
        String cropName,
        String cropVariety,
        Long plotId,
        String plotName,
        Double plotAreaM2,
        Integer treeCount,
        LocalDate plantingDate) {

    /** "Cà phê (Robusta)" hoặc "Ngô" nếu không có giống — giống {@code Crop.getDisplayName()}. */
    public String cropDisplayName() {
        return cropVariety == null ? cropName : "%s (%s)".formatted(cropName, cropVariety);
    }

    public int trees() {
        return treeCount == null ? 0 : treeCount;
    }
}
