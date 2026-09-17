package com.hmdao.farm.land.domain;

import static com.hmdao.farm.shared.domain.BusinessRuleViolationException.require;

import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Nông trại. Tham chiếu chủ sở hữu bằng id (không bằng entity {@code User}) để module
 * {@code land} không bị ràng buộc vào mô hình của module {@code identity}.
 */
@Entity
@Table(name = "farm")
public class Farm extends BaseEntity {

    public static final int NAME_MAX = 120;
    public static final int LOCATION_MAX = 255;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private Long ownerId;

    @Column(nullable = false, length = NAME_MAX)
    private String name;

    @Column(length = LOCATION_MAX)
    private String location;

    protected Farm() {
    }

    public static Farm create(Long ownerId, String name, String location) {
        Objects.requireNonNull(ownerId, "ownerId");
        Farm farm = new Farm();
        farm.ownerId = ownerId;
        farm.update(name, location);
        return farm;
    }

    public void update(String name, String location) {
        require(name != null && !name.isBlank(), "BR-01", "Tên nông trại không được để trống.");
        this.name = name.strip();
        this.location = location == null || location.isBlank() ? null : location.strip();
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }
}
