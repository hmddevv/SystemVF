package com.hmdao.farm.land.web.dto;

import com.hmdao.farm.land.application.dto.FarmView;

public record FarmResponse(Long id, String name, String location, long plotCount, double totalAreaM2) {

    public static FarmResponse from(FarmView view) {
        return new FarmResponse(view.id(), view.name(), view.location(), view.plotCount(), view.totalAreaM2());
    }
}
