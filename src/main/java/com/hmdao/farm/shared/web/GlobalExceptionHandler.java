package com.hmdao.farm.shared.web;

import com.hmdao.farm.shared.domain.BusinessRuleViolationException;
import com.hmdao.farm.shared.domain.DomainException;
import com.hmdao.farm.shared.domain.ResourceConflictException;
import com.hmdao.farm.shared.domain.ResourceNotFoundException;
import com.hmdao.farm.shared.domain.UnauthenticatedException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Nơi duy nhất quyết định mã HTTP cho lỗi. Mọi lỗi trả về dạng RFC 9457 ProblemDetail.
 *
 * <ul>
 *   <li>400 — sai cú pháp / Bean Validation trên request</li>
 *   <li>401 — không xác định được người dùng</li>
 *   <li>404 — không tìm thấy hoặc không thuộc chủ sở hữu (BR-11)</li>
 *   <li>409 — trùng lặp, còn dữ liệu con, xung đột cập nhật đồng thời</li>
 *   <li>422 — vi phạm quy tắc nghiệp vụ</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE = "urn:farm:problem:";

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return domainProblem(HttpStatus.NOT_FOUND, "not-found", "Không tìm thấy tài nguyên", ex);
    }

    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleConflict(ResourceConflictException ex) {
        return domainProblem(HttpStatus.CONFLICT, "conflict", "Xung đột dữ liệu", ex);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException ex) {
        return domainProblem(HttpStatus.UNPROCESSABLE_CONTENT, "business-rule", "Vi phạm quy tắc nghiệp vụ", ex);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ProblemDetail handleUnauthenticated(UnauthenticatedException ex) {
        return domainProblem(HttpStatus.UNAUTHORIZED, "unauthenticated", "Chưa xác định người dùng", ex);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Dữ liệu vừa được thay đổi bởi thao tác khác. Hãy tải lại rồi thực hiện lại.");
        problem.setType(URI.create(PROBLEM_BASE + "concurrent-update"));
        problem.setTitle("Xung đột cập nhật đồng thời");
        return problem;
    }

    /** Lớp phòng thủ cuối: ràng buộc DB (FK RESTRICT, UNIQUE) bị vi phạm do race condition. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc cơ sở dữ liệu: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Thao tác vi phạm ràng buộc dữ liệu: bản ghi bị trùng hoặc vẫn còn dữ liệu phụ thuộc.");
        problem.setType(URI.create(PROBLEM_BASE + "data-integrity"));
        problem.setTitle("Xung đột dữ liệu");
        return problem;
    }

    /**
     * Tham số sai kiểu: {@code ?groupBy=XYZ} hay {@code /plantings/abc}. Mặc định Spring trả
     * "Failed to convert value of type..." — tiếng Anh, lộ tên lớp nội bộ và không nói ra giá
     * trị nào mới đúng. Người gọi API cần biết điều cuối cùng đó.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? "'" + mismatch.getName() + "'"
                : "tham số";
        ProblemDetail problem = badRequest("Tham số không hợp lệ",
                "Giá trị '%s' không dùng được cho %s.%s".formatted(
                        ex.getValue(), name, expectedForm(ex.getRequiredType())));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Thân request không đọc được: JSON sai cú pháp, hoặc enum/ngày sai định dạng. Bean Validation
     * không bao giờ chạy tới trong trường hợp này vì object còn chưa dựng được, nên nếu không xử
     * lý riêng thì client nhận một thông điệp của Jackson.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detail = ex.getCause() instanceof InvalidFormatException invalid
                ? "Giá trị '%s' không dùng được cho trường '%s'.%s".formatted(
                        invalid.getValue(), fieldOf(invalid), expectedForm(invalid.getTargetType()))
                : "Thân request không phải JSON hợp lệ.";
        ProblemDetail problem = badRequest("Dữ liệu không hợp lệ", detail);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static String fieldOf(InvalidFormatException ex) {
        return ex.getPath().stream()
                .map(reference -> reference.getPropertyName() == null
                        ? "[" + reference.getIndex() + "]"
                        : reference.getPropertyName())
                .collect(Collectors.joining("."));
    }

    /** Nói thẳng dạng giá trị được chấp nhận — với enum là liệt kê đủ, vì danh sách hữu hạn. */
    private static String expectedForm(Class<?> required) {
        if (required == null) {
            return "";
        }
        if (required.isEnum()) {
            return " Giá trị hợp lệ: %s.".formatted(
                    Arrays.stream(required.getEnumConstants()).map(String::valueOf)
                            .collect(Collectors.joining(", ")));
        }
        if (required == LocalDate.class) {
            return " Cần ngày dạng YYYY-MM-DD.";
        }
        if (Number.class.isAssignableFrom(required) || required.isPrimitive()) {
            return " Cần một giá trị số.";
        }
        return "";
    }

    private static ProblemDetail badRequest(String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(URI.create(PROBLEM_BASE + "validation"));
        problem.setTitle(title);
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toViolation)
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không hợp lệ. Xem chi tiết từng trường trong 'errors'.");
        problem.setType(URI.create(PROBLEM_BASE + "validation"));
        problem.setTitle("Dữ liệu không hợp lệ");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private static FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private static ProblemDetail domainProblem(HttpStatus status, String type, String title, DomainException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + type));
        problem.setTitle(title);
        problem.setProperty("rule", ex.getRuleCode());
        return problem;
    }

    public record FieldViolation(String field, String message) {
    }
}
