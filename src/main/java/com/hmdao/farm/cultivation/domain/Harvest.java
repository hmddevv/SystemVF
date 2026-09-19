package com.hmdao.farm.cultivation.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Một lần thu hoạch: sản lượng và doanh thu của niên vụ chứa ngày thu hoạch. Một niên vụ có
 * nhiều lần thu hoạch — cà phê hái nhiều đợt, sầu riêng cắt theo lứa chín.
 */
@Entity
@Table(name = "harvest")
public class Harvest extends BaseEntity {

    public static final int MONEY_SCALE = 2;

    /** Sửa ngày thu hoạch sang chu kỳ khác thì bản ghi chuyển niên vụ (BR-05a). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Column(name = "quantity_kg", nullable = false)
    private double quantityKg;

    @Column(nullable = false, precision = 15, scale = MONEY_SCALE)
    private BigDecimal revenue;

    protected Harvest() {
    }

    public static Harvest record(Season season, LocalDate harvestDate, double quantityKg, BigDecimal revenue) {
        Harvest harvest = new Harvest();
        harvest.apply(season, harvestDate, quantityKg, revenue);
        return harvest;
    }

    /** Sửa dữ liệu nhập sai; niên vụ do service tính lại vì ngày mới có thể thuộc chu kỳ khác. */
    public void correct(Season season, LocalDate harvestDate, double quantityKg, BigDecimal revenue) {
        apply(season, harvestDate, quantityKg, revenue);
    }

    private void apply(Season season, LocalDate harvestDate, double quantityKg, BigDecimal revenue) {
        Objects.requireNonNull(season, "season");
        Objects.requireNonNull(harvestDate, "harvestDate");
        require(season.contains(harvestDate), "BR-07",
                "Ngày %s không nằm trong niên vụ %s (%s – %s).".formatted(harvestDate, season.getLabel(),
                        season.getStartDate(), season.getEndDate()));
        require(Double.isFinite(quantityKg) && quantityKg > 0, "BR-08",
                "Sản lượng phải lớn hơn 0 kg (nhận %s).".formatted(quantityKg));
        BigDecimal amount = (revenue == null ? BigDecimal.ZERO : revenue).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        require(amount.signum() >= 0, "BR-08", "Doanh thu không được âm (nhận %s).".formatted(amount));
        this.season = season;
        this.harvestDate = harvestDate;
        this.quantityKg = quantityKg;
        this.revenue = amount;
    }

    public Season getSeason() {
        return season;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public double getQuantityKg() {
        return quantityKg;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
