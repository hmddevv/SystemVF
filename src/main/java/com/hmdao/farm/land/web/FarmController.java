package com.hmdao.farm.land.web;

import com.hmdao.farm.land.application.dto.FarmView;
import com.hmdao.farm.land.application.port.in.FarmQueryUseCase;
import com.hmdao.farm.land.application.port.in.ManageFarmUseCase;
import com.hmdao.farm.land.web.dto.FarmRequest;
import com.hmdao.farm.land.web.dto.FarmResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Nông trại", description = "Epic A — quản lý tập trung nhiều nông trại")
@RestController
@RequestMapping("/api/v1/farms")
class FarmController {

    private final ManageFarmUseCase manageFarm;
    private final FarmQueryUseCase farmQuery;

    FarmController(ManageFarmUseCase manageFarm, FarmQueryUseCase farmQuery) {
        this.manageFarm = manageFarm;
        this.farmQuery = farmQuery;
    }

    @Operation(summary = "Danh sách nông trại của tôi", description = "Kèm số lô và tổng diện tích mỗi nông trại.")
    @GetMapping
    List<FarmResponse> list() {
        return farmQuery.listMine().stream().map(FarmResponse::from).toList();
    }

    @Operation(summary = "Chi tiết nông trại")
    @GetMapping("/{farmId}")
    FarmResponse get(@PathVariable Long farmId) {
        return FarmResponse.from(farmQuery.getById(farmId));
    }

    @Operation(summary = "Tạo nông trại")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<FarmResponse> create(@Valid @RequestBody FarmRequest request) {
        FarmView created = manageFarm.create(request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/farms/" + created.id())).body(FarmResponse.from(created));
    }

    @Operation(summary = "Sửa nông trại")
    @PutMapping("/{farmId}")
    FarmResponse update(@PathVariable Long farmId, @Valid @RequestBody FarmRequest request) {
        return FarmResponse.from(manageFarm.update(farmId, request.toCommand()));
    }

    @Operation(summary = "Xóa nông trại", description = "Trả 409 nếu nông trại còn lô đất (BR-10).")
    @DeleteMapping("/{farmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long farmId) {
        manageFarm.delete(farmId);
    }
}
