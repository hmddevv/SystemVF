package com.hmdao.farm.catalog.web.dto;

import com.hmdao.farm.catalog.application.dto.CropView;

public record CropResponse(Long id, String name, String variety, String displayName, boolean perennial,
        Integer seasonStartMonth) {

    public static CropResponse from(CropView view) {
        return new CropResponse(view.id(), view.name(), view.variety(), view.displayName(), view.perennial(),
                view.seasonStartMonth());
    }
}
