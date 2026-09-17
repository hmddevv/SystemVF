package com.hmdao.farm.land.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Lô đất trong một nông trại. Một lô chứa nhiều lứa trồng — đó là cách hệ thống biểu diễn
 * xen canh và lịch sử sử dụng đất.
 */
@Entity
@Table(name = "plot")
public class Plot extends BaseEntity {

    public static final int NAME_MAX = 60;
    public static final int SOIL_TYPE_MAX = 60;
    private static final double M2_PER_HECTARE = 10_000d;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false, updatable = false)
    private Farm farm;

    @Column(nullable = false, length = NAME_MAX)
    private String name;

    @Column(name = "area_m2", nullable = false)
    private double areaM2;

    @Column(name = "soil_type", length = SOIL_TYPE_MAX)
    private String soilType;

    protected Plot() {
    }

    public static Plot create(Farm farm, String name, double areaM2, String soilType) {
        Objects.requireNonNull(farm, "farm");
        Plot plot = new Plot();
        plot.farm = farm;
        plot.update(name, areaM2, soilType);
        return plot;
    }

    /** BR-01: tên lô không trống, diện tích lớn hơn 0. */
    public void update(String name, double areaM2, String soilType) {
        require(name != null && !name.isBlank(), "BR-01", "Tên lô đất không được để trống.");
        require(Double.isFinite(areaM2) && areaM2 > 0, "BR-01", "Diện tích lô đất phải lớn hơn 0 m².");
        this.name = name.strip();
        this.areaM2 = areaM2;
        this.soilType = soilType == null || soilType.isBlank() ? null : soilType.strip();
    }

    public double getAreaHectares() {
        return areaM2 / M2_PER_HECTARE;
    }

    public Farm getFarm() {
        return farm;
    }

    public String getName() {
        return name;
    }

    public double getAreaM2() {
        return areaM2;
    }

    public String getSoilType() {
        return soilType;
    }
}
