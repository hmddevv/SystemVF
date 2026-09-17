package com.hmdao.farm.land.application.dto;

/**
 * Số lô và tổng diện tích của một nông trại, tính bằng một truy vấn GROUP BY cho nhiều
 * nông trại cùng lúc (tránh N+1 khi hiển thị danh sách).
 */
public record FarmLandSummary(Long farmId, Long plotCount, Double totalAreaM2) {

    public static FarmLandSummary empty(Long farmId) {
        return new FarmLandSummary(farmId, 0L, 0d);
    }
}
