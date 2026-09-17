package com.hmdao.farm.land.application.dto;

import com.hmdao.farm.land.domain.Farm;

public record FarmView(Long id, String name, String location, long plotCount, double totalAreaM2) {

    public static FarmView of(Farm farm, FarmLandSummary summary) {
        return new FarmView(farm.getId(), farm.getName(), farm.getLocation(),
                summary.plotCount(), summary.totalAreaM2());
    }
}
