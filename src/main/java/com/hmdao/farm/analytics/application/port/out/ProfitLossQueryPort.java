package com.hmdao.farm.analytics.application.port.out;

import com.hmdao.farm.analytics.application.dto.PlantingProfile;
import com.hmdao.farm.analytics.application.dto.SeasonCost;
import com.hmdao.farm.analytics.application.dto.SeasonYield;
import java.util.Collection;
import java.util.List;

/**
 * Đọc dữ liệu đã tổng hợp sẵn cho báo cáo — cố tình không dùng lại repository của
 * {@code cultivation}: đọc và ghi có hình dạng dữ liệu khác nhau (ADR-11).
 *
 * <p>Ba method này là toàn bộ số truy vấn của một báo cáo, bất kể nông trại có bao nhiêu lô,
 * lứa trồng hay năm dữ liệu.
 */
public interface ProfitLossQueryPort {

    /** Mọi lứa trồng trong phạm vi, kể cả lứa chưa ghi gì (BR-14). {@code farmId} null = mọi nông trại. */
    List<PlantingProfile> findPlantings(Long ownerId, Long farmId);

    List<SeasonCost> costsByPlantingAndSeason(Collection<Long> plantingIds);

    List<SeasonYield> yieldsByPlantingAndSeason(Collection<Long> plantingIds);
}
