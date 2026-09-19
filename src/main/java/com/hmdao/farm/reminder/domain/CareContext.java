package com.hmdao.farm.reminder.domain;

import com.hmdao.farm.cultivation.domain.ActivityType;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Ảnh chụp mọi thứ một {@link CareRule} cần, dựng sẵn bởi service.
 *
 * <p>Luật không được tự truy vấn: nếu mỗi luật tự hỏi DB thì N lứa trồng × M luật sẽ thành
 * N×M truy vấn, và luật cũng không test được nếu không có database.
 *
 * @param lastActivity ngày làm gần nhất theo từng loại việc; loại chưa từng làm thì vắng mặt
 * @param lastHarvest  lần thu hoạch gần nhất; {@code null} nếu chưa thu hoạch bao giờ
 */
public record CareContext(
        Long plantingId,
        String cropName,
        String plotName,
        boolean perennial,
        LocalDate plantingDate,
        PlantingStatus status,
        int ageMonths,
        int treeCount,
        Map<ActivityType, LocalDate> lastActivity,
        LocalDate lastHarvest,
        LocalDate today) {

    public CareContext {
        lastActivity = Map.copyOf(lastActivity);
    }

    public Optional<LocalDate> lastDone(ActivityType type) {
        return Optional.ofNullable(lastActivity.get(type));
    }

    public Optional<LocalDate> lastDoneAnything() {
        return lastActivity.values().stream().max(LocalDate::compareTo);
    }

    /**
     * Ngày đến hạn của một việc làm định kỳ: lần cuối cộng chu kỳ, hoặc hôm nay nếu chưa từng
     * làm — vườn chưa có bản ghi nào thì việc đó đang tới hạn, không phải còn xa.
     */
    public LocalDate nextDue(ActivityType type, int intervalDays) {
        return lastDone(type).map(last -> last.plusDays(intervalDays)).orElse(today);
    }

    /** Tháng hiện tại nằm trong khoảng mùa vụ; xử lý cả khoảng vắt qua giao thừa như 12–4. */
    public boolean todayInMonths(int fromMonth, int toMonth) {
        int month = today.getMonthValue();
        return fromMonth <= toMonth
                ? month >= fromMonth && month <= toMonth
                : month >= fromMonth || month <= toMonth;
    }
}
