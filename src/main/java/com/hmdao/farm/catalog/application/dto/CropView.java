package com.hmdao.farm.catalog.application.dto;

import com.hmdao.farm.catalog.domain.Crop;

public record CropView(Long id, String name, String variety, String displayName, boolean perennial,
        Integer seasonStartMonth) {

    public static CropView of(Crop crop) {
        return new CropView(crop.getId(), crop.getName(), crop.getVariety(), crop.getDisplayName(),
                crop.isPerennial(), crop.getSeasonStartMonth());
    }
}
