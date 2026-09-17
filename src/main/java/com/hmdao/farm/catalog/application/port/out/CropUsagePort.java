package com.hmdao.farm.catalog.application.port.out;

import java.util.Optional;

/**
 * Đảo ngược phụ thuộc giữa {@code catalog} và {@code cultivation}: catalog khai báo,
 * cultivation implement (M2). Xem {@code PlotUsagePort} cho cùng mẫu thiết kế.
 */
public interface CropUsagePort {

    /** @return mô tả việc sử dụng (vd. "12 lứa trồng"), hoặc rỗng nếu chưa được dùng */
    Optional<String> describeUsage(Long cropId);
}
