package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.CorrectPlantingCommand;
import com.hmdao.farm.cultivation.application.dto.PlantingView;

/** Sửa dữ liệu nhập sai. Muốn "bỏ" một lứa trồng thật thì dùng terminate, không xóa. */
public interface CorrectPlantingUseCase {

    PlantingView correct(Long plantingId, CorrectPlantingCommand command);

    /** Chỉ xóa được lứa trồng tạo nhầm, chưa có niên vụ (BR-10). */
    void delete(Long plantingId);
}
