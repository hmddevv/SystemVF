package com.hmdao.farm.cultivation.web;

import com.hmdao.farm.cultivation.application.dto.PlantingView;
import com.hmdao.farm.cultivation.application.port.in.CorrectPlantingUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantCropUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantingLifecycleUseCase;
import com.hmdao.farm.cultivation.application.port.in.PlantingQueryUseCase;
import com.hmdao.farm.cultivation.web.dto.CorrectPlantingRequest;
import com.hmdao.farm.cultivation.web.dto.PlantCropRequest;
import com.hmdao.farm.cultivation.web.dto.PlantingResponse;
import com.hmdao.farm.cultivation.web.dto.TerminatePlantingRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. Lứa trồng", description = "Epic B, C — trồng xen canh và vòng đời lứa trồng")
@RestController
@RequestMapping("/api/v1")
class PlantingController {

    private final PlantCropUseCase plantCrop;
    private final PlantingLifecycleUseCase lifecycle;
    private final CorrectPlantingUseCase correction;
    private final PlantingQueryUseCase query;

    PlantingController(PlantCropUseCase plantCrop, PlantingLifecycleUseCase lifecycle,
            CorrectPlantingUseCase correction, PlantingQueryUseCase query) {
        this.plantCrop = plantCrop;
        this.lifecycle = lifecycle;
        this.correction = correction;
        this.query = query;
    }

    @Operation(summary = "Lô đất đang có những cây gì",
            description = "activeOnly=true (mặc định): lứa đang canh tác. activeOnly=false: toàn bộ lịch sử sử dụng đất.")
    @GetMapping("/plots/{plotId}/plantings")
    List<PlantingResponse> listByPlot(@PathVariable Long plotId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return query.listByPlot(plotId, activeOnly).stream().map(PlantingResponse::from).toList();
    }

    @Operation(summary = "Mọi lứa trồng của tôi",
            description = "Cho form ghi nhật ký chọn lứa trồng trong một lần gọi. farmId bỏ trống: mọi nông trại. "
                    + "Nông trại của người khác trả 404 (BR-11). Xếp theo tên lô, lứa mới trồng trước.")
    @GetMapping("/plantings")
    List<PlantingResponse> listOwned(@RequestParam(required = false) Long farmId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return query.listOwned(farmId, activeOnly).stream().map(PlantingResponse::from).toList();
    }

    @Operation(summary = "Trồng cây lên lô đất",
            description = "Gọi nhiều lần trên cùng lô để trồng xen canh. Ngày trồng không ở tương lai, số cây > 0 (BR-02).")
    @PostMapping("/plots/{plotId}/plantings")
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<PlantingResponse> plant(@PathVariable Long plotId, @Valid @RequestBody PlantCropRequest request) {
        PlantingView created = plantCrop.plant(plotId, request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/plantings/" + created.id()))
                .body(PlantingResponse.from(created));
    }

    @Operation(summary = "Chi tiết lứa trồng")
    @GetMapping("/plantings/{plantingId}")
    PlantingResponse get(@PathVariable Long plantingId) {
        return PlantingResponse.from(query.getById(plantingId));
    }

    @Operation(summary = "Sửa ngày trồng, số cây (nhập sai)",
            description = "Không đổi được cây trồng hay lô đất — đổi cây là một lứa trồng mới.")
    @PutMapping("/plantings/{plantingId}")
    PlantingResponse correct(@PathVariable Long plantingId, @Valid @RequestBody CorrectPlantingRequest request) {
        return PlantingResponse.from(correction.correct(plantingId, request.toCommand()));
    }

    @Operation(summary = "Xóa lứa trồng tạo nhầm",
            description = "Muốn bỏ một lứa trồng thật (cưa bỏ, chặt bỏ) thì dùng /termination để giữ lịch sử.")
    @DeleteMapping("/plantings/{plantingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long plantingId) {
        correction.delete(plantingId);
    }

    @Operation(summary = "Bắt đầu cho thu hoạch", description = "GROWING → PRODUCING (BR-03).")
    @PostMapping("/plantings/{plantingId}/production-start")
    PlantingResponse startProducing(@PathVariable Long plantingId) {
        return PlantingResponse.from(lifecycle.startProducing(plantingId));
    }

    @Operation(summary = "Kết thúc lứa trồng (cưa bỏ)",
            description = "Bắt buộc ngày và lý do; ngày kết thúc ≥ ngày trồng và ≤ hôm nay (BR-04). "
                    + "Lứa trồng đã kết thúc không chuyển trạng thái được nữa (BR-03).")
    @PostMapping("/plantings/{plantingId}/termination")
    PlantingResponse terminate(@PathVariable Long plantingId, @Valid @RequestBody TerminatePlantingRequest request) {
        return PlantingResponse.from(lifecycle.terminate(plantingId, request.toCommand()));
    }
}
