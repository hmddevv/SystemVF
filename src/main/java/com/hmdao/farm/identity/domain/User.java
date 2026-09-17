package com.hmdao.farm.identity.domain;

import com.hmdao.farm.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Người dùng — ở MVP là chủ nông trại. Bảng tên {@code app_user} vì {@code user} là từ khóa
 * của PostgreSQL.
 */
@Entity
@Table(name = "app_user")
public class User extends BaseEntity {

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    protected User() {
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
