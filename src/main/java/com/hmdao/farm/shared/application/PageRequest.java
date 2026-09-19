package com.hmdao.farm.shared.application;

/**
 * Yêu cầu phân trang của tầng application — cố tình không dùng {@code Pageable} của Spring Data
 * để input port và use case không dính vào framework lưu trữ (ADR-8). Adapter persistence chuyển
 * đổi sang {@code Pageable} ngay tại biên.
 *
 * <p>Giá trị ngoài khoảng cho phép được kẹp lại thay vì báo lỗi: đây là tham số tiện ích của
 * danh sách, không phải dữ liệu nghiệp vụ.
 */
public record PageRequest(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    public PageRequest {
        page = Math.max(page, 0);
        size = Math.clamp(size, 1, MAX_SIZE);
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    public static PageRequest first() {
        return new PageRequest(0, DEFAULT_SIZE);
    }

    public int offset() {
        return page * size;
    }
}
