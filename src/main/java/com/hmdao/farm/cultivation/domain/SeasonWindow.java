package com.hmdao.farm.cultivation.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Cửa sổ thời gian của một niên vụ, do {@link SeasonPolicy} tính ra từ loại cây và ngày ghi
 * nhận. Là value object: không có định danh, không lưu trực tiếp — {@link Season} mới là bản ghi.
 *
 * @param year      năm bắt đầu niên vụ; niên vụ 1/2/2025 – 31/1/2026 có {@code year = 2025}
 * @param startDate ngày đầu niên vụ, đã cắt theo ngày trồng của lứa
 * @param endDate   ngày cuối niên vụ; {@code null} nghĩa là đang diễn ra, không có cận trên
 */
public record SeasonWindow(int year, LocalDate startDate, LocalDate endDate) {

    public SeasonWindow {
        Objects.requireNonNull(startDate, "startDate");
        require(endDate == null || !endDate.isBefore(startDate), "BR-06",
                "Niên vụ %d kết thúc ngày %s trước khi bắt đầu ngày %s.".formatted(year, endDate, startDate));
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }
}
