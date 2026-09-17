package com.hmdao.farm.land.application.dto;

import com.hmdao.farm.land.domain.Plot;

public record PlotView(Long id, Long farmId, String name, double areaM2, double areaHectares, String soilType) {

    public static PlotView of(Plot plot) {
        return new PlotView(plot.getId(), plot.getFarm().getId(), plot.getName(),
                plot.getAreaM2(), plot.getAreaHectares(), plot.getSoilType());
    }
}
