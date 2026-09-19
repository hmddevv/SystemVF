package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.cultivation.application.dto.HarvestView;
import com.hmdao.farm.cultivation.application.dto.RecordHarvestCommand;
import com.hmdao.farm.cultivation.application.port.in.HarvestQueryUseCase;
import com.hmdao.farm.cultivation.application.port.in.RecordHarvestUseCase;
import com.hmdao.farm.cultivation.application.port.out.HarvestRepository;
import com.hmdao.farm.cultivation.domain.Harvest;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import com.hmdao.farm.cultivation.domain.Season;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class HarvestService implements RecordHarvestUseCase, HarvestQueryUseCase {

    private final HarvestRepository harvests;
    private final SeasonAssigner seasonAssigner;
    private final CultivationAccess access;
    private final Clock clock;

    HarvestService(HarvestRepository harvests, SeasonAssigner seasonAssigner, CultivationAccess access, Clock clock) {
        this.harvests = harvests;
        this.seasonAssigner = seasonAssigner;
        this.access = access;
        this.clock = clock;
    }

    @Override
    public HarvestView record(Long plantingId, RecordHarvestCommand command) {
        Planting planting = access.planting(plantingId);
        Season season = seasonFor(planting, command.harvestDate());
        Harvest harvest = harvests.save(
                Harvest.record(season, command.harvestDate(), command.quantityKg(), command.revenue()));
        // BR-09: có thu hoạch nghĩa là vườn đã vào giai đoạn kinh doanh. Chiều ngược lại không
        // tự động (ADR-9) — xóa nhầm một lần thu hoạch không được âm thầm đổi trạng thái lứa trồng.
        if (planting.getStatus() == PlantingStatus.GROWING) {
            planting.startProducing();
        }
        return HarvestView.of(harvest);
    }

    @Override
    public HarvestView correct(Long harvestId, RecordHarvestCommand command) {
        Harvest harvest = access.harvest(harvestId);
        Season season = seasonFor(harvest.getSeason().getPlanting(), command.harvestDate());
        harvest.correct(season, command.harvestDate(), command.quantityKg(), command.revenue());
        return HarvestView.of(harvest);
    }

    @Override
    public void delete(Long harvestId) {
        harvests.delete(access.harvest(harvestId));
    }

    @Override
    @Transactional(readOnly = true)
    public HarvestView getById(Long harvestId) {
        return HarvestView.of(access.harvest(harvestId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HarvestView> listBySeason(Long seasonId, PageRequest request) {
        access.season(seasonId);
        return harvests.findPageBySeasonId(seasonId, request).map(HarvestView::of);
    }

    private Season seasonFor(Planting planting, LocalDate date) {
        planting.requireRecordable(date, LocalDate.now(clock));
        return seasonAssigner.resolve(planting, date);
    }
}
