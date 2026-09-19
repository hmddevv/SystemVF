package com.hmdao.farm.cultivation.domain;

import com.hmdao.farm.catalog.domain.Crop;
import java.time.LocalDate;
import java.time.Month;

/**
 * Cây lâu năm: chu kỳ sản xuất bắt đầu ở {@code CROP.season_start_month} và kéo dài một năm.
 *
 * <p>Cà phê Robusta bắt đầu tháng 2, nên ngày 15/1/2026 thuộc niên vụ 1/2/2025 – 31/1/2026,
 * tức {@code year = 2025}. Nhờ vậy chi phí tưới tháng 2–3 và doanh thu thu hoạch tháng 11–1
 * nằm cùng một niên vụ thay vì bị cắt đôi theo năm dương lịch.
 *
 * <p>Cận dưới luôn cắt theo ngày trồng: niên vụ đầu tiên của một vườn trồng giữa vụ bắt đầu
 * từ ngày trồng, không phải từ đầu chu kỳ (BR-06).
 */
public class PerennialSeasonPolicy implements SeasonPolicy {

    @Override
    public boolean appliesTo(Crop crop) {
        return crop.getSeasonStart() != null;
    }

    @Override
    public SeasonWindow windowContaining(Planting planting, LocalDate date) {
        LocalDate plantingDate = planting.getPlantingDate();
        Month seasonStart = planting.getCrop().getSeasonStart();
        LocalDate cycleStart = date.withDayOfMonth(1).withMonth(seasonStart.getValue());
        if (cycleStart.isAfter(date)) {
            cycleStart = cycleStart.minusYears(1);
        }
        LocalDate openedOn = cycleStart.isBefore(plantingDate) ? plantingDate : cycleStart;
        return new SeasonWindow(cycleStart.getYear(), openedOn, cycleStart.plusYears(1).minusDays(1));
    }
}
