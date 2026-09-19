package com.hmdao.farm.cultivation.application.dto;

import java.math.BigDecimal;

/**
 * Tổng hợp nhật ký của một niên vụ, lấy bằng một truy vấn {@code GROUP BY} cho cả danh sách
 * niên vụ cùng lúc (tránh N+1). Niên vụ chưa có hoạt động nào sẽ vắng mặt trong kết quả.
 */
public record ActivityTotals(Long seasonId, Long entries, BigDecimal totalCost) {

    /** Kiểu bọc để khớp đúng chữ ký {@code count()}/{@code sum()} của JPQL constructor expression. */
    public ActivityTotals {
        entries = entries == null ? 0L : entries;
        totalCost = totalCost == null ? BigDecimal.ZERO : totalCost;
    }

    public static ActivityTotals empty(Long seasonId) {
        return new ActivityTotals(seasonId, 0L, BigDecimal.ZERO);
    }
}
