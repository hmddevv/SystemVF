package com.hmdao.farm.cultivation.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Niên vụ: một chu kỳ sản xuất của một lứa trồng. Cây ngắn ngày có đúng một niên vụ; cây lâu
 * năm có nhiều niên vụ nối tiếp nhau, nhờ đó so sánh được năng suất và lãi/lỗ qua từng năm.
 *
 * <p>Bản ghi do hệ thống sinh ra theo {@link SeasonPolicy} khi có hoạt động hoặc lần thu hoạch
 * đầu tiên rơi vào chu kỳ đó (BR-05a) — không có API tạo tay (ADR-7).
 */
@Entity
@Table(name = "season")
public class Season extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planting_id", nullable = false, updatable = false)
    private Planting planting;

    /** Năm bắt đầu niên vụ, duy nhất trong một lứa trồng (BR-05). */
    @Column(name = "year", nullable = false, updatable = false)
    private int year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** {@code null} = đang diễn ra, không có cận trên (cây ngắn ngày). */
    @Column(name = "end_date")
    private LocalDate endDate;

    protected Season() {
    }

    /** Mở niên vụ theo cửa sổ đã tính. Dùng {@code SeasonAssigner}, không gọi trực tiếp từ web. */
    public static Season open(Planting planting, SeasonWindow window) {
        Objects.requireNonNull(planting, "planting");
        Objects.requireNonNull(window, "window");
        require(!window.startDate().isBefore(planting.getPlantingDate()), "BR-06",
                "Niên vụ %d bắt đầu ngày %s, trước ngày trồng %s."
                        .formatted(window.year(), window.startDate(), planting.getPlantingDate()));
        Season season = new Season();
        season.planting = planting;
        season.year = window.year();
        season.startDate = window.startDate();
        season.endDate = window.endDate();
        return season;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && (endDate == null || !date.isAfter(endDate));
    }

    /** "2025/2026" khi niên vụ vắt qua hai năm dương lịch, ngược lại chỉ "2025". */
    public String getLabel() {
        return endDate != null && endDate.getYear() != year
                ? "%d/%d".formatted(year, endDate.getYear())
                : String.valueOf(year);
    }

    public Planting getPlanting() {
        return planting;
    }

    public int getYear() {
        return year;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }
}
