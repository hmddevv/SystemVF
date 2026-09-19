package com.hmdao.farm.cultivation.application.port.out;

import com.hmdao.farm.cultivation.application.dto.ActivityTotals;
import com.hmdao.farm.cultivation.domain.Activity;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityRepository {

    Activity save(Activity activity);

    /** Nạp sẵn niên vụ, lứa trồng và cây trồng (BR-11 đi qua cả bốn cấp). */
    Optional<Activity> findByIdAndSeasonPlantingPlotFarmOwnerId(Long id, Long ownerId);

    /** Nhật ký một niên vụ, mới nhất trước, có phân trang. */
    Page<Activity> findPageBySeasonId(Long seasonId, PageRequest request);

    long countBySeasonId(Long seasonId);

    /** Một truy vấn GROUP BY cho tất cả niên vụ cần hiển thị. Niên vụ chưa có hoạt động sẽ vắng mặt. */
    List<ActivityTotals> totalsBySeasonIds(Collection<Long> seasonIds);

    void delete(Activity activity);
}
