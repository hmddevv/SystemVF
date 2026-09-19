package com.hmdao.farm.shared.application;

import java.util.List;
import java.util.function.Function;

/**
 * Một trang kết quả kèm tổng số bản ghi. Xem {@link PageRequest} về lý do không dùng kiểu
 * {@code Page} của Spring Data.
 *
 * @param <T> kiểu phần tử; {@link #map(Function)} đổi entity sang view mà vẫn giữ thông tin trang
 */
public record Page<T>(List<T> content, int page, int size, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }

    public static <T> Page<T> empty(PageRequest request) {
        return new Page<>(List.of(), request.page(), request.size(), 0);
    }

    public int totalPages() {
        return size <= 0 ? 0 : (int) ((totalElements + size - 1) / size);
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new Page<>(mapped, page, size, totalElements);
    }
}
