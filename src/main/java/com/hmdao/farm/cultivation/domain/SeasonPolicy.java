package com.hmdao.farm.cultivation.domain;

import com.hmdao.farm.catalog.domain.Crop;
import java.time.LocalDate;

/**
 * Quy tắc suy ra niên vụ từ một ngày ghi nhận (BR-05a). Niên vụ là dữ liệu dẫn xuất — người
 * dùng không nhập, hệ thống tính (ADR-7).
 *
 * <p>Mỗi nhóm cây có một chu kỳ sản xuất khác nhau, nên đây là điểm mở rộng: thêm một chu kỳ
 * mới là thêm một implementation, không sửa {@code SeasonAssigner} (Open/Closed). Mọi
 * implementation tuân cùng hợp đồng: cửa sổ trả về phải chứa {@code date} và không bắt đầu
 * trước ngày trồng (Liskov).
 */
public interface SeasonPolicy {

    /** Chính sách này áp dụng cho loại cây nào. Các chính sách phải loại trừ lẫn nhau. */
    boolean appliesTo(Crop crop);

    /** Cửa sổ niên vụ chứa {@code date}. Gọi được cho mọi ngày hợp lệ theo BR-07. */
    SeasonWindow windowContaining(Planting planting, LocalDate date);
}
