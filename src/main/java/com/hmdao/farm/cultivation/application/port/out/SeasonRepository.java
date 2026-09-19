package com.hmdao.farm.cultivation.application.port.out;

import com.hmdao.farm.cultivation.domain.Season;
import java.util.List;
import java.util.Optional;

/**
 * Output port lưu trữ niên vụ. Quyền sở hữu kiểm tra qua chuỗi lứa trồng → lô → nông trại (BR-11).
 */
public interface SeasonRepository {

    Season save(Season season);

    /** Nạp sẵn lứa trồng và cây trồng — view niên vụ luôn cần tên cây. */
    Optional<Season> findByIdAndPlantingPlotFarmOwnerId(Long id, Long ownerId);

    /** BR-05: khóa tự nhiên của niên vụ trong một lứa trồng; dùng cho việc tự gán (BR-05a). */
    Optional<Season> findByPlantingIdAndYear(Long plantingId, int year);

    /** Mới nhất trước — nhà nông quan tâm vụ đang chạy. */
    List<Season> findAllByPlantingIdOrderByYearDesc(Long plantingId);

    long countByPlantingId(Long plantingId);

    void delete(Season season);
}
