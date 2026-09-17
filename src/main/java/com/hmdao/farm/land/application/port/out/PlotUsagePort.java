package com.hmdao.farm.land.application.port.out;

import java.util.Optional;

/**
 * Hỏi "lô đất này có đang được module khác sử dụng không" mà không phụ thuộc vào module đó.
 *
 * <p>Module {@code cultivation} phụ thuộc {@code land}, nên {@code land} không được import
 * {@code cultivation}. Đảo ngược phụ thuộc: {@code land} khai báo interface này,
 * {@code cultivation} implement nó (M2) — đồ thị module vẫn không có chu trình.
 */
public interface PlotUsagePort {

    /** @return mô tả lý do lô đang được sử dụng (vd. "3 lứa trồng"), hoặc rỗng nếu không */
    Optional<String> describeUsage(Long plotId);
}
