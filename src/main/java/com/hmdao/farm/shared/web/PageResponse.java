package com.hmdao.farm.shared.web;

import com.hmdao.farm.shared.application.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Hình dạng chung cho mọi danh sách có phân trang.
 *
 * @param <T> kiểu phần tử của trang
 */
public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Trang hiện tại, đánh số từ 0", example = "0") int page,
        @Schema(description = "Số bản ghi mỗi trang, tối đa 200", example = "20") int size,
        @Schema(description = "Tổng số bản ghi khớp điều kiện", example = "137") long totalElements,
        @Schema(example = "7") int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.content(), page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
