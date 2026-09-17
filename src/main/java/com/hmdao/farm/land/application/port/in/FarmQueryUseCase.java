package com.hmdao.farm.land.application.port.in;

import com.hmdao.farm.land.application.dto.FarmView;
import java.util.List;

public interface FarmQueryUseCase {

    FarmView getById(Long farmId);

    List<FarmView> listMine();
}
