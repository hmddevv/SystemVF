package com.hmdao.farm.cultivation.domain;

import java.time.LocalDate;
import java.time.Month;

/**
 * Quy tắc suy ra niên vụ từ một ngày ghi nhận (BR-05a). Niên vụ là dữ liệu dẫn xuất — người
 * dùng không nhập, hệ thống tính (ADR-7).
 *
 * <ul>
 *   <li><b>Cây lâu năm</b> — chu kỳ sản xuất bắt đầu ở {@code CROP.season_start_month} và kéo
 *       dài một năm. Cà phê Robusta bắt đầu tháng 2, nên ngày 15/1/2026 thuộc niên vụ
 *       1/2/2025 – 31/1/2026, tức {@code year = 2025}. Nhờ vậy chi phí tưới tháng 2–3 và doanh
 *       thu thu hoạch tháng 11–1 nằm cùng một niên vụ thay vì bị cắt đôi theo năm dương lịch.</li>
 *   <li><b>Cây ngắn ngày</b> — không có tháng bắt đầu niên vụ; cả lứa trồng chỉ có đúng một
 *       niên vụ, mở từ ngày trồng và không có cận trên (BR-05).</li>
 * </ul>
 *
 * <p>Cận dưới luôn được cắt theo ngày trồng: niên vụ đầu tiên của một vườn trồng giữa vụ bắt
 * đầu từ ngày trồng, không phải từ đầu chu kỳ (BR-06).
 */
public final class SeasonPolicy {

    private SeasonPolicy() {
    }

    /** Cửa sổ niên vụ chứa {@code date}. Gọi được cho mọi ngày hợp lệ theo BR-07. */
    public static SeasonWindow windowContaining(Planting planting, LocalDate date) {
        LocalDate plantingDate = planting.getPlantingDate();
        Month seasonStart = planting.getCrop().getSeasonStart();
        if (seasonStart == null) {
            return new SeasonWindow(plantingDate.getYear(), plantingDate, null);
        }
        LocalDate cycleStart = date.withDayOfMonth(1).withMonth(seasonStart.getValue());
        if (cycleStart.isAfter(date)) {
            cycleStart = cycleStart.minusYears(1);
        }
        LocalDate openedOn = cycleStart.isBefore(plantingDate) ? plantingDate : cycleStart;
        return new SeasonWindow(cycleStart.getYear(), openedOn, cycleStart.plusYears(1).minusDays(1));
    }
}
