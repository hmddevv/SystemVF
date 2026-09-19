package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.SeasonView;
import java.util.List;

/** Niên vụ chỉ đọc và xóa — bản ghi do hệ thống sinh ra theo BR-05a, không có API tạo tay (ADR-7). */
public interface SeasonQueryUseCase {

    SeasonView getById(Long seasonId);

    /** Epic E — các vụ của một lứa trồng kèm chi phí, sản lượng, doanh thu để so sánh theo năm. */
    List<SeasonView> listByPlanting(Long plantingId);
}
