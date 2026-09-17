package com.hmdao.farm.land.application.port.in;

import com.hmdao.farm.land.application.dto.PlotView;
import java.util.List;

public interface PlotQueryUseCase {

    PlotView getById(Long plotId);

    List<PlotView> listByFarm(Long farmId);
}
