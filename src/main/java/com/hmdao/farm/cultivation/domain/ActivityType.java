package com.hmdao.farm.cultivation.domain;

/**
 * Loại hoạt động chăm sóc — enum thay cho chuỗi tự do để thống kê được chi phí theo loại việc.
 */
public enum ActivityType {
    /** Tưới nước. */
    WATERING,
    /** Bón phân. */
    FERTILIZING,
    /** Phun thuốc bảo vệ thực vật. */
    SPRAYING,
    /** Làm cỏ. */
    WEEDING,
    /** Tỉa cành, tạo tán. */
    PRUNING,
    /** Việc khác — bắt buộc kèm ghi chú (BR-12). */
    OTHER
}
