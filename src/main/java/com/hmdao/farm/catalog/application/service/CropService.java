package com.hmdao.farm.catalog.application.service;

import com.hmdao.farm.catalog.application.dto.CropCommand;
import com.hmdao.farm.catalog.application.dto.CropView;
import com.hmdao.farm.catalog.application.port.in.CropQueryUseCase;
import com.hmdao.farm.catalog.application.port.in.ManageCropUseCase;
import com.hmdao.farm.catalog.application.port.out.CropRepository;
import com.hmdao.farm.catalog.application.port.out.CropUsagePort;
import com.hmdao.farm.catalog.domain.Crop;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
class CropService implements ManageCropUseCase, CropQueryUseCase {

    private final CropRepository crops;
    private final List<CropUsagePort> usagePorts;

    CropService(CropRepository crops, List<CropUsagePort> usagePorts) {
        this.crops = crops;
        this.usagePorts = usagePorts;
    }

    @Override
    public CropView create(CropCommand command) {
        Crop crop = Crop.create(command.name(), command.variety(), command.perennial(), command.seasonStartMonth());
        if (crops.existsDuplicate(crop.getName(), crop.getVariety())) {
            throw duplicate(crop.getDisplayName());
        }
        return CropView.of(crops.save(crop));
    }

    @Override
    public CropView update(Long cropId, CropCommand command) {
        Crop crop = load(cropId);
        // Kiểm tra trước khi sửa entity để auto-flush không đẩy dữ liệu trùng xuống DB.
        Crop candidate = Crop.create(command.name(), command.variety(), command.perennial(), command.seasonStartMonth());
        if (crops.existsDuplicateExcluding(candidate.getName(), candidate.getVariety(), cropId)) {
            throw duplicate(candidate.getDisplayName());
        }
        if (crop.isPerennial() != candidate.isPerennial()) {
            findUsage(cropId).ifPresent(usage -> {
                throw new ResourceConflictException("BR-05",
                        "Không thể đổi '%s' giữa cây lâu năm và cây ngắn ngày vì đã có %s — cách chia niên vụ của dữ liệu cũ sẽ sai."
                                .formatted(crop.getDisplayName(), usage));
            });
        }
        crop.update(command.name(), command.variety(), command.perennial(), command.seasonStartMonth());
        return CropView.of(crop);
    }

    @Override
    public void delete(Long cropId) {
        Crop crop = load(cropId);
        findUsage(cropId).ifPresent(usage -> {
            throw new ResourceConflictException("BR-10",
                    "Cây trồng '%s' đang được dùng trong %s. Không thể xóa để bảo toàn lịch sử canh tác."
                            .formatted(crop.getDisplayName(), usage));
        });
        crops.delete(crop);
    }

    @Override
    @Transactional(readOnly = true)
    public CropView getById(Long cropId) {
        return CropView.of(load(cropId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CropView> listAll() {
        return crops.findAllByOrderByNameAscVarietyAsc().stream().map(CropView::of).toList();
    }

    private Optional<String> findUsage(Long cropId) {
        return usagePorts.stream()
                .map(port -> port.describeUsage(cropId))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Crop load(Long cropId) {
        return crops.findById(cropId).orElseThrow(() -> new ResourceNotFoundException("cây trồng", cropId));
    }

    private static ResourceConflictException duplicate(String displayName) {
        return new ResourceConflictException("BR-01", "Danh mục đã có cây trồng '%s'.".formatted(displayName));
    }
}
