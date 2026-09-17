package com.hmdao.farm.cultivation.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Trạng thái lứa trồng và bảng chuyển trạng thái hợp lệ (BR-03).
 *
 * <pre>
 * GROWING ──► PRODUCING ──► TERMINATED
 *    └──────────────────────────▲
 * </pre>
 */
public enum PlantingStatus {

    /** Kiến thiết cơ bản — chưa cho thu hoạch. */
    GROWING,
    /** Kinh doanh — đang cho thu hoạch. */
    PRODUCING,
    /** Đã kết thúc (cưa bỏ, chặt bỏ). Trạng thái cuối. */
    TERMINATED;

    public boolean canTransitionTo(PlantingStatus target) {
        return allowedTargets().contains(target);
    }

    private Set<PlantingStatus> allowedTargets() {
        return switch (this) {
            case GROWING -> EnumSet.of(PRODUCING, TERMINATED);
            case PRODUCING -> EnumSet.of(TERMINATED);
            case TERMINATED -> EnumSet.noneOf(PlantingStatus.class);
        };
    }
}
