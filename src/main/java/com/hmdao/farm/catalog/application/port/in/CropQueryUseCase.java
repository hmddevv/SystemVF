package com.hmdao.farm.catalog.application.port.in;

import com.hmdao.farm.catalog.application.dto.CropView;
import java.util.List;

public interface CropQueryUseCase {

    CropView getById(Long cropId);

    List<CropView> listAll();
}
