package com.hmdao.farm.land.application.port.in;

import com.hmdao.farm.land.application.dto.FarmCommand;
import com.hmdao.farm.land.application.dto.FarmView;

/** Epic A — tạo, sửa, xóa nông trại của chủ sở hữu hiện tại. */
public interface ManageFarmUseCase {

    FarmView create(FarmCommand command);

    FarmView update(Long farmId, FarmCommand command);

    /** @throws com.hmdao.farm.shared.domain.ResourceConflictException nếu nông trại còn lô đất (BR-10) */
    void delete(Long farmId);
}
