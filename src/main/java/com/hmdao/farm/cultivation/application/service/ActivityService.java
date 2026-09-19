package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.cultivation.application.dto.ActivityView;
import com.hmdao.farm.cultivation.application.dto.LogActivityCommand;
import com.hmdao.farm.cultivation.application.port.in.ActivityQueryUseCase;
import com.hmdao.farm.cultivation.application.port.in.LogActivityUseCase;
import com.hmdao.farm.cultivation.application.port.out.ActivityRepository;
import com.hmdao.farm.cultivation.domain.Activity;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class ActivityService implements LogActivityUseCase, ActivityQueryUseCase {

    private final ActivityRepository activities;
    private final SeasonAssigner seasonAssigner;
    private final CultivationAccess access;
    private final Clock clock;

    ActivityService(ActivityRepository activities, SeasonAssigner seasonAssigner, CultivationAccess access,
            Clock clock) {
        this.activities = activities;
        this.seasonAssigner = seasonAssigner;
        this.access = access;
        this.clock = clock;
    }

    @Override
    public ActivityView log(Long plantingId, LogActivityCommand command) {
        Planting planting = access.planting(plantingId);
        Season season = seasonFor(planting, command.activityDate());
        Activity activity = Activity.log(season, command.type(), command.activityDate(), command.cost(),
                command.note());
        return ActivityView.of(activities.save(activity));
    }

    @Override
    public ActivityView correct(Long activityId, LogActivityCommand command) {
        Activity activity = access.activity(activityId);
        // Ngày mới có thể rơi sang chu kỳ khác — tính lại niên vụ thay vì giữ nguyên niên vụ cũ (BR-05a).
        Season season = seasonFor(activity.getSeason().getPlanting(), command.activityDate());
        activity.correct(season, command.type(), command.activityDate(), command.cost(), command.note());
        return ActivityView.of(activity);
    }

    @Override
    public void delete(Long activityId) {
        activities.delete(access.activity(activityId));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityView getById(Long activityId) {
        return ActivityView.of(access.activity(activityId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityView> listBySeason(Long seasonId, PageRequest request) {
        access.season(seasonId);
        return activities.findPageBySeasonId(seasonId, request).map(ActivityView::of);
    }

    private Season seasonFor(Planting planting, LocalDate date) {
        planting.requireRecordable(date, LocalDate.now(clock));
        return seasonAssigner.resolve(planting, date);
    }
}
