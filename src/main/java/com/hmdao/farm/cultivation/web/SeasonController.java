package com.hmdao.farm.cultivation.web;

import com.hmdao.farm.cultivation.application.port.in.ManageSeasonUseCase;
import com.hmdao.farm.cultivation.application.port.in.SeasonQueryUseCase;
import com.hmdao.farm.cultivation.web.dto.SeasonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "5. Niên vụ", description = """
        Chu kỳ sản xuất của một lứa trồng. Niên vụ do hệ thống tự tạo khi ghi hoạt động hoặc \
        thu hoạch, theo tháng bắt đầu niên vụ của loại cây — vì vậy không có API tạo hay sửa.""")
@RestController
@RequestMapping("/api/v1")
class SeasonController {

    private final SeasonQueryUseCase query;
    private final ManageSeasonUseCase management;

    SeasonController(SeasonQueryUseCase query, ManageSeasonUseCase management) {
        this.query = query;
        this.management = management;
    }

    @Operation(summary = "Các niên vụ của một lứa trồng",
            description = "Mới nhất trước, kèm tổng chi phí, sản lượng, doanh thu và lãi/lỗ để so sánh theo năm.")
    @GetMapping("/plantings/{plantingId}/seasons")
    List<SeasonResponse> listByPlanting(@PathVariable Long plantingId) {
        return query.listByPlanting(plantingId).stream().map(SeasonResponse::from).toList();
    }

    @Operation(summary = "Chi tiết một niên vụ")
    @GetMapping("/seasons/{seasonId}")
    SeasonResponse get(@PathVariable Long seasonId) {
        return SeasonResponse.from(query.getById(seasonId));
    }

    @Operation(summary = "Xóa niên vụ rỗng",
            description = "Chỉ xóa được khi không còn hoạt động và thu hoạch nào (BR-10).")
    @DeleteMapping("/seasons/{seasonId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long seasonId) {
        management.delete(seasonId);
    }
}
