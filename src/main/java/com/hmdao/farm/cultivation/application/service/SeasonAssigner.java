package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.cultivation.domain.SeasonPolicy;
import com.hmdao.farm.cultivation.domain.SeasonWindow;
import java.time.LocalDate;
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

    SeasonAssigner(SeasonRepository seasons) {
        this.seasons = seasons;
    }

    Season resolve(Planting planting, LocalDate date) {
        SeasonWindow window = SeasonPolicy.windowContaining(planting, date);
        return seasons.findByPlantingIdAndYear(planting.getId(), window.year())
                .orElseGet(() -> seasons.save(Season.open(planting, window)));
    }
}
