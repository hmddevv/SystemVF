package com.hmdao.farm.cultivation.domain;

/**
 * Lý do kết thúc lứa trồng — enum thay cho chuỗi tự do để thống kê được vì sao nhà nông
 * chuyển đổi cây trồng.
 */
public enum EndReason {
    /** Giá thị trường không còn hiệu quả, chuyển sang cây khác. */
    MARKET,
    /** Sâu bệnh, ví dụ hồ tiêu chết nhanh, chết chậm. */
    PEST_DISEASE,
    /** Hạn hán, lũ, gió bão. */
    WEATHER,
    /** Cây già cỗi, năng suất thấp, cần tái canh. */
    OLD_AGE,
    OTHER
}
