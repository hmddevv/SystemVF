package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.ActivityView;
import com.hmdao.farm.cultivation.application.dto.LogActivityCommand;

/** Epic D — nhật ký canh tác. Ghi vào lứa trồng; niên vụ do hệ thống tự gán theo ngày (BR-05a). */
public interface LogActivityUseCase {

    ActivityView log(Long plantingId, LogActivityCommand command);

    /** Sửa nhập sai. Đổi ngày sang chu kỳ khác thì bản ghi tự chuyển niên vụ. */
    ActivityView correct(Long activityId, LogActivityCommand command);

    void delete(Long activityId);
}
