package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.PlantingView;
import java.util.List;

public interface PlantingQueryUseCase {

    PlantingView getById(Long plantingId);

    /**
     * Epic B — lô đất đang có những cây gì.
     *
     * @param activeOnly {@code true}: chỉ lứa đang canh tác; {@code false}: toàn bộ lịch sử sử dụng đất
     */
    List<PlantingView> listByPlot(Long plotId, boolean activeOnly);
}
