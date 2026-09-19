package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.cultivation.application.port.out.ActivityRepository;
import com.hmdao.farm.cultivation.application.port.out.HarvestRepository;
import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.application.port.out.SeasonRepository;
import com.hmdao.farm.cultivation.domain.Activity;
import com.hmdao.farm.cultivation.domain.Harvest;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Nạp dữ liệu canh tác theo chủ sở hữu hiện tại (BR-11). Ba service của M3 đều phải đi qua cùng
 * một chuỗi lứa trồng → lô → nông trại → chủ sở hữu, nên chuỗi đó nằm ở một chỗ duy nhất.
 *
 * <p>Không tìm thấy và không thuộc quyền sở hữu đều trả 404 như nhau, để không lộ sự tồn tại
 * dữ liệu của người khác.
 */
@Component
class CultivationAccess {

    private final PlantingRepository plantings;
    private final SeasonRepository seasons;
    private final ActivityRepository activities;
    private final HarvestRepository harvests;
    private final CurrentUserProvider currentUser;

    CultivationAccess(PlantingRepository plantings, SeasonRepository seasons, ActivityRepository activities,
            HarvestRepository harvests, CurrentUserProvider currentUser) {
        this.plantings = plantings;
        this.seasons = seasons;
        this.activities = activities;
        this.harvests = harvests;
        this.currentUser = currentUser;
    }

    Planting planting(Long plantingId) {
        return plantings.findByIdAndPlotFarmOwnerId(plantingId, owner())
                .orElseThrow(() -> new ResourceNotFoundException("lứa trồng", plantingId));
    }

    Season season(Long seasonId) {
        return seasons.findByIdAndPlantingPlotFarmOwnerId(seasonId, owner())
                .orElseThrow(() -> new ResourceNotFoundException("niên vụ", seasonId));
    }

    Activity activity(Long activityId) {
        return activities.findByIdAndSeasonPlantingPlotFarmOwnerId(activityId, owner())
                .orElseThrow(() -> new ResourceNotFoundException("hoạt động", activityId));
    }

    Harvest harvest(Long harvestId) {
        return harvests.findByIdAndSeasonPlantingPlotFarmOwnerId(harvestId, owner())
                .orElseThrow(() -> new ResourceNotFoundException("lần thu hoạch", harvestId));
    }

    private Long owner() {
        return currentUser.currentUserId();
    }
}
