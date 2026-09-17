package com.hmdao.farm.catalog.application.port.in;

import com.hmdao.farm.catalog.application.dto.CropCommand;
import com.hmdao.farm.catalog.application.dto.CropView;

public interface ManageCropUseCase {

    CropView create(CropCommand command);

    /** @throws com.hmdao.farm.shared.domain.ResourceConflictException nếu đổi loại cây (lâu năm/ngắn ngày) khi đã có lứa trồng */
    CropView update(Long cropId, CropCommand command);

    /** @throws com.hmdao.farm.shared.domain.ResourceConflictException nếu cây đã có lứa trồng (BR-10) */
    void delete(Long cropId);
}
