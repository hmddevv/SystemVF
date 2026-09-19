package com.hmdao.farm.cultivation.web;

import com.hmdao.farm.cultivation.application.dto.HarvestView;
import com.hmdao.farm.cultivation.application.port.in.HarvestQueryUseCase;
import com.hmdao.farm.cultivation.application.port.in.RecordHarvestUseCase;
import com.hmdao.farm.cultivation.web.dto.HarvestResponse;
import com.hmdao.farm.cultivation.web.dto.RecordHarvestRequest;
import com.hmdao.farm.shared.application.PageRequest;
import com.hmdao.farm.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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

@Tag(name = "7. Thu hoạch", description = """
        Epic E — sản lượng và doanh thu mỗi lần thu hoạch. Một niên vụ có nhiều lần: \
        cà phê hái nhiều đợt, sầu riêng cắt theo lứa chín.""")
@RestController
@RequestMapping("/api/v1")
class HarvestController {

    private final RecordHarvestUseCase recording;
    private final HarvestQueryUseCase query;

    HarvestController(RecordHarvestUseCase recording, HarvestQueryUseCase query) {
        this.recording = recording;
        this.query = query;
    }

    @Operation(summary = "Ghi nhận một lần thu hoạch",
            description = "Niên vụ được tự gán theo ngày thu hoạch (BR-05a). Lần thu hoạch đầu tiên "
                    + "đưa lứa đang GROWING sang PRODUCING (BR-09).")
    @PostMapping("/plantings/{plantingId}/harvests")
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<HarvestResponse> record(@PathVariable Long plantingId,
            @Valid @RequestBody RecordHarvestRequest request) {
        HarvestView created = recording.record(plantingId, request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/harvests/" + created.id()))
                .body(HarvestResponse.from(created));
    }

    @Operation(summary = "Các lần thu hoạch của một niên vụ", description = "Mới nhất trước, có phân trang.")
    @GetMapping("/seasons/{seasonId}/harvests")
    PageResponse<HarvestResponse> listBySeason(@PathVariable Long seasonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(query.listBySeason(seasonId, PageRequest.of(page, size))
                .map(HarvestResponse::from));
    }

    @Operation(summary = "Chi tiết một lần thu hoạch")
    @GetMapping("/harvests/{harvestId}")
    HarvestResponse get(@PathVariable Long harvestId) {
        return HarvestResponse.from(query.getById(harvestId));
    }

    @Operation(summary = "Sửa lần thu hoạch nhập sai",
            description = "Không đảo ngược trạng thái lứa trồng về GROWING (BR-09).")
    @PutMapping("/harvests/{harvestId}")
    HarvestResponse correct(@PathVariable Long harvestId, @Valid @RequestBody RecordHarvestRequest request) {
        return HarvestResponse.from(recording.correct(harvestId, request.toCommand()));
    }

    @Operation(summary = "Xóa lần thu hoạch nhập sai")
    @DeleteMapping("/harvests/{harvestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long harvestId) {
        recording.delete(harvestId);
    }
}
