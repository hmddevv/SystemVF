package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.cultivation.application.dto.ActivityTotals;
import com.hmdao.farm.cultivation.application.dto.HarvestTotals;
import com.hmdao.farm.cultivation.application.dto.SeasonView;
import com.hmdao.farm.cultivation.application.port.in.ManageSeasonUseCase;
import com.hmdao.farm.cultivation.application.port.in.SeasonQueryUseCase;
import com.hmdao.farm.cultivation.application.port.out.ActivityRepository;
import com.hmdao.farm.cultivation.application.port.out.HarvestRepository;
import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class SeasonService implements SeasonQueryUseCase, ManageSeasonUseCase {

    private final SeasonRepository seasons;
    private final ActivityRepository activities;
    private final HarvestRepository harvests;
    private final CultivationAccess access;

    SeasonService(SeasonRepository seasons, ActivityRepository activities, HarvestRepository harvests,
            CultivationAccess access) {
        this.seasons = seasons;
        this.activities = activities;
        this.harvests = harvests;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public SeasonView getById(Long seasonId) {
        return summarize(List.of(access.season(seasonId))).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeasonView> listByPlanting(Long plantingId) {
        access.planting(plantingId);
        return summarize(seasons.findAllByPlantingIdOrderByYearDesc(plantingId));
    }

    @Override
    public void delete(Long seasonId) {
        Season season = access.season(seasonId);
        long activityCount = activities.countBySeasonId(seasonId);
        long harvestCount = harvests.countBySeasonId(seasonId);
        if (activityCount + harvestCount > 0) {
            throw new ResourceConflictException("BR-10",
                    "Niên vụ %s còn %d hoạt động và %d lần thu hoạch. Xóa các bản ghi đó trước nếu thật sự nhập nhầm."
                            .formatted(season.getLabel(), activityCount, harvestCount));
        }
        seasons.delete(season);
    }

    /**
     * Ghép niên vụ với số tổng hợp bằng đúng hai truy vấn {@code GROUP BY} cho cả danh sách —
     * cộng dồn trong Java sẽ phải tải toàn bộ nhật ký, còn gộp hai bảng vào một câu SQL sẽ nhân
     * chéo dòng và cộng sai tổng.
     */
    private List<SeasonView> summarize(List<Season> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<Long> ids = found.stream().map(Season::getId).toList();
        Map<Long, ActivityTotals> costs = index(activities.totalsBySeasonIds(ids), ActivityTotals::seasonId);
        Map<Long, HarvestTotals> yields = index(harvests.totalsBySeasonIds(ids), HarvestTotals::seasonId);
        return found.stream()
                .map(season -> SeasonView.of(season,
                        costs.getOrDefault(season.getId(), ActivityTotals.empty(season.getId())),
                        yields.getOrDefault(season.getId(), HarvestTotals.empty(season.getId()))))
                .toList();
    }

    private static <T> Map<Long, T> index(List<T> totals, Function<T, Long> key) {
        return totals.stream().collect(Collectors.toMap(key, Function.identity()));
    }
}
