package com.hmdao.farm.land.web.dto;

import com.hmdao.farm.land.application.dto.PlotView;

public record PlotResponse(Long id, Long farmId, String name, double areaM2, double areaHectares, String soilType) {

    public static PlotResponse from(PlotView view) {
        return new PlotResponse(view.id(), view.farmId(), view.name(), view.areaM2(), view.areaHectares(), view.soilType());
    }
}
