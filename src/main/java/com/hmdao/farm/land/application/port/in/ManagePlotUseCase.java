package com.hmdao.farm.land.application.port.in;

import com.hmdao.farm.land.application.dto.PlotCommand;
import com.hmdao.farm.land.application.dto.PlotView;

/** Epic A — thêm, sửa, xóa lô đất trong nông trại. */
public interface ManagePlotUseCase {

    PlotView create(Long farmId, PlotCommand command);

    PlotView update(Long plotId, PlotCommand command);

    /** @throws com.hmdao.farm.shared.domain.ResourceConflictException nếu lô còn lứa trồng (BR-10) */
    void delete(Long plotId);
}
