package com.hmdao.farm.land.web.dto;

import com.hmdao.farm.land.application.dto.FarmCommand;
import com.hmdao.farm.land.domain.Farm;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FarmRequest(
        @Schema(example = "Nông trại Cư M'gar")
        @NotBlank(message = "Tên nông trại không được để trống")
        @Size(max = Farm.NAME_MAX, message = "Tên nông trại tối đa {max} ký tự")
        String name,

        @Schema(example = "Xã Ea Tar, huyện Cư M'gar, Đắk Lắk")
        @Size(max = Farm.LOCATION_MAX, message = "Địa điểm tối đa {max} ký tự")
        String location) {

    public FarmCommand toCommand() {
        return new FarmCommand(name, location);
    }
}
