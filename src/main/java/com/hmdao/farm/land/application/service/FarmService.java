package com.hmdao.farm.land.application.service;

import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import com.hmdao.farm.land.application.dto.FarmCommand;
import com.hmdao.farm.land.application.dto.FarmLandSummary;
import com.hmdao.farm.land.application.dto.FarmView;
import com.hmdao.farm.land.application.port.in.FarmQueryUseCase;
import com.hmdao.farm.land.application.port.in.ManageFarmUseCase;
import com.hmdao.farm.land.application.port.out.FarmRepository;
import com.hmdao.farm.land.application.port.out.PlotRepository;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class FarmService implements ManageFarmUseCase, FarmQueryUseCase {

    private final FarmRepository farms;
    private final PlotRepository plots;
    private final CurrentUserProvider currentUser;

    FarmService(FarmRepository farms, PlotRepository plots, CurrentUserProvider currentUser) {
        this.farms = farms;
        this.plots = plots;
        this.currentUser = currentUser;
    }

    @Override
    public FarmView create(FarmCommand command) {
        Farm farm = farms.save(Farm.create(currentUser.currentUserId(), command.name(), command.location()));
        return FarmView.of(farm, FarmLandSummary.empty(farm.getId()));
    }

    @Override
    public FarmView update(Long farmId, FarmCommand command) {
        Farm farm = loadOwned(farmId);
        farm.update(command.name(), command.location());
        return toView(farm);
    }

    @Override
    public void delete(Long farmId) {
        Farm farm = loadOwned(farmId);
        long plotCount = plots.countByFarmId(farmId);
        if (plotCount > 0) {
            throw new ResourceConflictException("BR-10",
                    "Nông trại '%s' còn %d lô đất. Hãy xóa các lô đất trước khi xóa nông trại."
                            .formatted(farm.getName(), plotCount));
        }
        farms.delete(farm);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmView getById(Long farmId) {
        return toView(loadOwned(farmId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmView> listMine() {
        List<Farm> owned = farms.findAllByOwnerIdOrderByNameAsc(currentUser.currentUserId());
        if (owned.isEmpty()) {
            return List.of();
        }
        Map<Long, FarmLandSummary> summaries = plots.summarizeByFarmIds(owned.stream().map(Farm::getId).toList())
                .stream()
                .collect(Collectors.toMap(FarmLandSummary::farmId, Function.identity()));
        return owned.stream()
                .map(farm -> FarmView.of(farm, summaries.getOrDefault(farm.getId(), FarmLandSummary.empty(farm.getId()))))
                .toList();
    }

    private FarmView toView(Farm farm) {
        FarmLandSummary summary = plots.summarizeByFarmIds(List.of(farm.getId())).stream()
                .findFirst()
                .orElseGet(() -> FarmLandSummary.empty(farm.getId()));
        return FarmView.of(farm, summary);
    }

    private Farm loadOwned(Long farmId) {
        return farms.findByIdAndOwnerId(farmId, currentUser.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("nông trại", farmId));
    }
}
