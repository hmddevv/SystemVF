package com.hmdao.farm.cultivation.application.service;

import com.hmdao.farm.catalog.application.port.out.CropRepository;
import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.cultivation.application.dto.CorrectPlantingCommand;
import com.hmdao.farm.cultivation.application.dto.PlantCropCommand;
import com.hmdao.farm.cultivation.application.dto.PlantingView;
import com.hmdao.farm.cultivation.application.dto.TerminatePlantingCommand;
import com.hmdao.farm.cultivation.application.port.in.CorrectPlantingUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantCropUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantingLifecycleUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantingQueryUseCase;
import com.hmdao.farm.cultivation.application.port.out.PlantingRepository;
import com.hmdao.farm.cultivation.domain.Planting;
import com.hmdao.farm.cultivation.domain.PlantingStatus;
import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import com.hmdao.farm.land.application.port.out.PlotRepository;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PlantingService implements PlantCropUseCase, PlantingLifecycleUseCase, CorrectPlantingUseCase,
        PlantingQueryUseCase {

    private final PlantingRepository plantings;
    private final PlotRepository plots;
    private final CropRepository crops;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    PlantingService(PlantingRepository plantings, PlotRepository plots, CropRepository crops,
            CurrentUserProvider currentUser, Clock clock) {
        this.plantings = plantings;
        this.plots = plots;
        this.crops = crops;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public PlantingView plant(Long plotId, PlantCropCommand command) {
        Plot plot = loadOwnedPlot(plotId);
        Crop crop = crops.findById(command.cropId())
                .orElseThrow(() -> new ResourceNotFoundException("cây trồng", command.cropId()));
        Planting planting = Planting.plant(plot, crop, command.plantingDate(), command.treeCount(),
                command.alreadyProducing(), today());
        return view(plantings.save(planting));
    }

    @Override
    public PlantingView startProducing(Long plantingId) {
        Planting planting = loadOwned(plantingId);
        planting.startProducing();
        return view(planting);
    }

    @Override
    public PlantingView terminate(Long plantingId, TerminatePlantingCommand command) {
        Planting planting = loadOwned(plantingId);
        planting.terminate(command.endDate(), command.reason(), command.note(), today());
        return view(planting);
    }

    @Override
    public PlantingView correct(Long plantingId, CorrectPlantingCommand command) {
        Planting planting = loadOwned(plantingId);
        planting.correct(command.plantingDate(), command.treeCount(), today());
        return view(planting);
    }

    @Override
    public void delete(Long plantingId) {
        // M3 bổ sung: chặn xóa khi lứa trồng đã có niên vụ (BR-10).
        plantings.delete(loadOwned(plantingId));
    }

    @Override
    @Transactional(readOnly = true)
    public PlantingView getById(Long plantingId) {
        return view(loadOwned(plantingId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlantingView> listByPlot(Long plotId, boolean activeOnly) {
        loadOwnedPlot(plotId);
        List<Planting> found = activeOnly
                ? plantings.findAllByPlotIdAndStatusNotOrderByPlantingDateDescIdDesc(plotId, PlantingStatus.TERMINATED)
                : plantings.findAllByPlotIdOrderByPlantingDateDescIdDesc(plotId);
        LocalDate today = today();
        return found.stream().map(planting -> PlantingView.of(planting, today)).toList();
    }

    private PlantingView view(Planting planting) {
        return PlantingView.of(planting, today());
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private Plot loadOwnedPlot(Long plotId) {
        return plots.findByIdAndFarmOwnerId(plotId, currentUser.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("lô đất", plotId));
    }

    private Planting loadOwned(Long plantingId) {
        return plantings.findByIdAndPlotFarmOwnerId(plantingId, currentUser.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("lứa trồng", plantingId));
    }
}
