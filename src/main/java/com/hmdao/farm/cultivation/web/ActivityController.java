package com.hmdao.farm.cultivation.web;

import com.hmdao.farm.cultivation.application.dto.ActivityView;
import com.hmdao.farm.cultivation.application.port.in.ActivityQueryUseCase;
import com.hmdao.farm.cultivation.application.port.in.LogActivityUseCase;
import com.hmdao.farm.cultivation.web.dto.ActivityResponse;
import com.hmdao.farm.cultivation.web.dto.LogActivityRequest;
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

@Tag(name = "6. Nhật ký canh tác", description = """
        Epic D — tưới, bón phân, phun thuốc, làm cỏ, tỉa cành kèm ngày và chi phí. \
        Ghi vào lứa trồng, đọc theo niên vụ: nhà nông chỉ khai cây nào và ngày nào, \
        hệ thống tự xếp vào đúng niên vụ.""")
@RestController
@RequestMapping("/api/v1")
class ActivityController {

    private final LogActivityUseCase logging;
    private final ActivityQueryUseCase query;

    ActivityController(LogActivityUseCase logging, ActivityQueryUseCase query) {
        this.logging = logging;
        this.query = query;
    }

    @Operation(summary = "Ghi một hoạt động chăm sóc",
            description = "Niên vụ được tự gán theo ngày thực hiện (BR-05a); ngày phải trong khoảng "
                    + "từ ngày trồng đến hôm nay, và không sau ngày cưa bỏ (BR-07).")
    @PostMapping("/plantings/{plantingId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<ActivityResponse> log(@PathVariable Long plantingId,
            @Valid @RequestBody LogActivityRequest request) {
        ActivityView created = logging.log(plantingId, request.toCommand());
        return ResponseEntity.created(URI.create("/api/v1/activities/" + created.id()))
                .body(ActivityResponse.from(created));
    }

    @Operation(summary = "Nhật ký của một niên vụ", description = "Mới nhất trước, có phân trang.")
    @GetMapping("/seasons/{seasonId}/activities")
    PageResponse<ActivityResponse> listBySeason(@PathVariable Long seasonId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(query.listBySeason(seasonId, PageRequest.of(page, size))
                .map(ActivityResponse::from));
    }

    @Operation(summary = "Chi tiết một hoạt động")
    @GetMapping("/activities/{activityId}")
    ActivityResponse get(@PathVariable Long activityId) {
        return ActivityResponse.from(query.getById(activityId));
    }

    @Operation(summary = "Sửa hoạt động nhập sai",
            description = "Đổi ngày sang chu kỳ sản xuất khác thì bản ghi tự chuyển sang niên vụ tương ứng.")
    @PutMapping("/activities/{activityId}")
    ActivityResponse correct(@PathVariable Long activityId, @Valid @RequestBody LogActivityRequest request) {
        return ActivityResponse.from(logging.correct(activityId, request.toCommand()));
    }

    @Operation(summary = "Xóa hoạt động nhập sai",
            description = "Nhật ký được xóa thật vì đây là dữ liệu nhập tay, không phải sự kiện vòng đời.")
    @DeleteMapping("/activities/{activityId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long activityId) {
        logging.delete(activityId);
    }
}
