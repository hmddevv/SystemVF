package com.hmdao.farm.cultivation.domain;

import com.hmdao.farm.catalog.domain.Crop;
import java.time.LocalDate;

/**
 * Cây ngắn ngày: không có tháng bắt đầu niên vụ, cả lứa trồng chỉ có đúng một niên vụ (BR-05),
 * mở từ ngày trồng và không có cận trên — cận trên thật sự là ngày cưa bỏ, do BR-07 canh ở
 * tầng bản ghi.
 */
public class AnnualSeasonPolicy implements SeasonPolicy {

    @Override
    public boolean appliesTo(Crop crop) {
        return crop.getSeasonStart() == null;
    }

    @Override
    public SeasonWindow windowContaining(Planting planting, LocalDate date) {
        LocalDate plantingDate = planting.getPlantingDate();
        return new SeasonWindow(plantingDate.getYear(), plantingDate, null);
    }
}
