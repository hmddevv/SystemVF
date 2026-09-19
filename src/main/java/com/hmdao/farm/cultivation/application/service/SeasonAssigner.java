package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.cultivation.domain.SeasonPolicy;
import com.hmdao.farm.cultivation.domain.SeasonWindow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Biến ngày ghi nhận thành niên vụ (BR-05a): tính cửa sổ theo {@link SeasonPolicy}, lấy bản ghi
 * của năm đó nếu đã có, chưa có thì mở mới. Nhờ vậy nhà nông chỉ khai "cây nào, ngày nào".
 *
 * <p>Hai request ghi cùng lúc vào một niên vụ chưa tồn tại sẽ chạm {@code UNIQUE (planting_id,
 * year)} và nhận 409 thay vì tạo hai niên vụ trùng — đúng vai trò lớp phòng thủ cuối của DB.
 */
@Component
class SeasonAssigner {

    private final SeasonRepository seasons;
    private final List<SeasonPolicy> policies;

    SeasonAssigner(SeasonRepository seasons, List<SeasonPolicy> policies) {
        this.seasons = seasons;
        this.policies = List.copyOf(policies);
    }

    Season resolve(Planting planting, LocalDate date) {
        SeasonWindow window = policyFor(planting.getCrop()).windowContaining(planting, date);
        return seasons.findByPlantingIdAndYear(planting.getId(), window.year())
                .orElseGet(() -> seasons.save(Season.open(planting, window)));
    }

    private SeasonPolicy policyFor(Crop crop) {
        return policies.stream()
                .filter(policy -> policy.appliesTo(crop))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Không có chính sách niên vụ nào áp dụng cho cây '%s'.".formatted(crop.getDisplayName())));
    }
}
