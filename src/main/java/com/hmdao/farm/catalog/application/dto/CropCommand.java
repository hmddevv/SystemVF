package com.hmdao.farm.catalog.application.dto;

public record CropCommand(String name, String variety, boolean perennial, Integer seasonStartMonth) {
}
