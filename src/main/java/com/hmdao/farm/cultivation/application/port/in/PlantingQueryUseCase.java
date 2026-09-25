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

    /**
     * Mọi lứa trồng của chủ sở hữu hiện tại, xếp theo tên lô — cho form ghi nhật ký chọn lứa trồng
     * mà không phải gọi từng lô một (N+1 qua mạng, đúng lúc sóng ngoài vườn yếu nhất).
     *
     * @param farmId {@code null}: mọi nông trại của chủ sở hữu; nông trại của người khác → 404 (BR-11)
     */
    List<PlantingView> listOwned(Long farmId, boolean activeOnly);
}
