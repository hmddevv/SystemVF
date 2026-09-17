package com.hmdao.farm.land.web;

import com.hmdao.farm.land.application.dto.PlotView;
import com.hmdao.farm.land.application.port.in.ManagePlotUseCase;
import com.hmdao.farm.land.application.port.in.PlotQueryUseCase;
import com.hmdao.farm.land.web.dto.PlotRequest;
import com.hmdao.farm.land.web.dto.PlotResponse;
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

@Tag(name = "2. Lô đất", description = "Epic A — theo dõi riêng từng khu vực canh tác")
@RestController
@RequestMapping("/api/v1")
class PlotController {

    private final ManagePlotUseCase managePlot;
    private final PlotQueryUseCase plotQuery;

    PlotController(ManagePlotUseCase managePlot, PlotQueryUseCase plotQuery) {
        this.managePlot = managePlot;
        this.plotQuery = plotQuery;
    }

    @Operation(summary = "Danh sách lô đất của nông trại")
    @GetMapping("/farms/{farmId}/plots")
    List<PlotResponse> listByFarm(@PathVariable Long farmId) {
        return plotQuery.listByFarm(farmId).stream().map(PlotResponse::from).toList();
    }

    @Operation(summary = "Thêm lô đất vào nông trại",
            description = "Diện tích phải > 0 (BR-01); tên lô không trùng trong cùng nông trại (409).")
    @PostMapping("/farms/{farmId}/plots")
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<PlotResponse> create(@PathVariable Long farmId, @Valid @RequestBody PlotRequest request) {
        PlotView created = managePlot.create(farmId, request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/plots/" + created.id())).body(PlotResponse.from(created));
    }

    @Operation(summary = "Chi tiết lô đất")
    @GetMapping("/plots/{plotId}")
    PlotResponse get(@PathVariable Long plotId) {
        return PlotResponse.from(plotQuery.getById(plotId));
    }

    @Operation(summary = "Sửa lô đất")
    @PutMapping("/plots/{plotId}")
    PlotResponse update(@PathVariable Long plotId, @Valid @RequestBody PlotRequest request) {
        return PlotResponse.from(managePlot.update(plotId, request.toCommand()));
    }

    @Operation(summary = "Xóa lô đất", description = "Trả 409 nếu lô còn lứa trồng (BR-10).")
    @DeleteMapping("/plots/{plotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long plotId) {
        managePlot.delete(plotId);
    }
}
