package com.hmdao.farm.land.application.service;

import com.hmdao.farm.identity.application.port.CurrentUserProvider;
import com.hmdao.farm.land.application.dto.PlotCommand;
import com.hmdao.farm.land.application.dto.PlotView;
import com.hmdao.farm.land.application.port.in.ManagePlotUseCase;
import com.hmdao.farm.land.application.port.in.PlotQueryUseCase;
import com.hmdao.farm.land.application.port.out.FarmRepository;
import com.hmdao.farm.land.application.port.out.PlotRepository;
import com.hmdao.farm.land.application.port.out.PlotUsagePort;
import com.hmdao.farm.land.domain.Farm;
import com.hmdao.farm.land.domain.Plot;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class PlotService implements ManagePlotUseCase, PlotQueryUseCase {

    private final PlotRepository plots;
    private final FarmRepository farms;
    private final CurrentUserProvider currentUser;
    private final List<PlotUsagePort> usagePorts;

    PlotService(PlotRepository plots, FarmRepository farms, CurrentUserProvider currentUser,
            List<PlotUsagePort> usagePorts) {
        this.plots = plots;
        this.farms = farms;
        this.currentUser = currentUser;
        this.usagePorts = usagePorts;
    }

    @Override
    public PlotView create(Long farmId, PlotCommand command) {
        Farm farm = loadOwnedFarm(farmId);
        Plot plot = Plot.create(farm, command.name(), command.areaM2(), command.soilType());
        if (plots.existsByFarmIdAndNameIgnoreCase(farmId, plot.getName())) {
            throw duplicateName(farm, plot.getName());
        }
        return PlotView.of(plots.save(plot));
    }

    @Override
    public PlotView update(Long plotId, PlotCommand command) {
        Plot plot = loadOwned(plotId);
        Farm farm = plot.getFarm();
        // Kiểm tra trùng tên TRƯỚC khi sửa entity: nếu sửa trước, auto-flush của câu truy vấn
        // sẽ đẩy UPDATE xuống DB và lỗi UNIQUE xuất hiện thay cho thông báo nghiệp vụ.
        String newName = command.name() == null ? null : command.name().strip();
        if (newName != null && plots.existsByFarmIdAndNameIgnoreCaseAndIdNot(farm.getId(), newName, plotId)) {
            throw duplicateName(farm, newName);
        }
        plot.update(command.name(), command.areaM2(), command.soilType());
        return PlotView.of(plot);
    }

    @Override
    public void delete(Long plotId) {
        Plot plot = loadOwned(plotId);
        usagePorts.stream()
                .map(port -> port.describeUsage(plotId))
                .flatMap(Optional::stream)
                .findFirst()
                .ifPresent(usage -> {
                    throw new ResourceConflictException("BR-10",
                            "Lô đất '%s' có lịch sử canh tác: %s. Không thể xóa để bảo toàn lịch sử sử dụng đất."
                                    .formatted(plot.getName(), usage));
                });
        plots.delete(plot);
    }

    @Override
    @Transactional(readOnly = true)
    public PlotView getById(Long plotId) {
        return PlotView.of(loadOwned(plotId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlotView> listByFarm(Long farmId) {
        loadOwnedFarm(farmId);
        return plots.findAllByFarmIdOrderByNameAsc(farmId).stream().map(PlotView::of).toList();
    }

    private Farm loadOwnedFarm(Long farmId) {
        return farms.findByIdAndOwnerId(farmId, currentUser.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("nông trại", farmId));
    }

    private Plot loadOwned(Long plotId) {
        return plots.findByIdAndFarmOwnerId(plotId, currentUser.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("lô đất", plotId));
    }

    private static ResourceConflictException duplicateName(Farm farm, String plotName) {
        return new ResourceConflictException("BR-01",
                "Nông trại '%s' đã có lô đất tên '%s'. Hãy chọn tên khác.".formatted(farm.getName(), plotName));
    }
}
