package com.hmdao.farm.catalog.web;

import com.hmdao.farm.catalog.application.dto.CropView;
import com.hmdao.farm.catalog.application.port.in.CropQueryUseCase;
import com.hmdao.farm.catalog.application.port.in.ManageCropUseCase;
import com.hmdao.farm.catalog.web.dto.CropRequest;
import com.hmdao.farm.catalog.web.dto.CropResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3. Cây trồng", description = "Danh mục loại cây và tháng bắt đầu niên vụ")
@RestController
@RequestMapping("/api/v1/crops")
class CropController {

    private final ManageCropUseCase manageCrop;
    private final CropQueryUseCase cropQuery;

    CropController(ManageCropUseCase manageCrop, CropQueryUseCase cropQuery) {
        this.manageCrop = manageCrop;
        this.cropQuery = cropQuery;
    }

    @Operation(summary = "Danh mục cây trồng")
    @GetMapping
    List<CropResponse> list() {
        return cropQuery.listAll().stream().map(CropResponse::from).toList();
    }

    @Operation(summary = "Chi tiết cây trồng")
    @GetMapping("/{cropId}")
    CropResponse get(@PathVariable Long cropId) {
        return CropResponse.from(cropQuery.getById(cropId));
    }

    @Operation(summary = "Thêm cây trồng vào danh mục",
            description = "Cây lâu năm bắt buộc có tháng bắt đầu niên vụ; cây ngắn ngày để trống (BR-05a).")
    @PostMapping
    ResponseEntity<CropResponse> create(@Valid @RequestBody CropRequest request) {
        CropView created = manageCrop.create(request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/crops/" + created.id())).body(CropResponse.from(created));
    }

    @Operation(summary = "Sửa cây trồng")
    @PutMapping("/{cropId}")
    CropResponse update(@PathVariable Long cropId, @Valid @RequestBody CropRequest request) {
        return CropResponse.from(manageCrop.update(cropId, request.toCommand()));
    }

    @Operation(summary = "Xóa cây trồng", description = "Trả 409 nếu cây đã có lứa trồng (BR-10).")
    @DeleteMapping("/{cropId}")
    ResponseEntity<Void> delete(@PathVariable Long cropId) {
        manageCrop.delete(cropId);
        return ResponseEntity.noContent().build();
    }
}
