package com.hmdao.farm.cultivation.application.port.in;

import com.hmdao.farm.cultivation.application.dto.ActivityView;
import com.hmdao.farm.shared.application.Page;
import com.hmdao.farm.shared.application.PageRequest;

public interface ActivityQueryUseCase {

    ActivityView getById(Long activityId);

    Page<ActivityView> listBySeason(Long seasonId, PageRequest request);
}
