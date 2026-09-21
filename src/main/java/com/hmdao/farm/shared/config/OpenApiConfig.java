package com.hmdao.farm.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI farmOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Farm Management System API")
                .version("v1")
                .description("""
                        Quản lý canh tác nông trại: nông trại, lô đất, danh mục cây trồng, lứa trồng \
                        xen canh, nhật ký chăm sóc, thu hoạch và lãi/lỗ theo từng cây.

                        Lỗi trả về dạng RFC 9457 ProblemDetail; trường `rule` là mã quy tắc nghiệp vụ \
                        trong docs/architecture.md."""));
    }

    /**
     * Lỗi 400 do lớp cha {@code ResponseEntityExceptionHandler} xử lý nên springdoc không tự
     * phát hiện được. Bổ sung cho mọi thao tác nhận dữ liệu từ người gọi — thân request
     * (Bean Validation) hoặc tham số đường dẫn/truy vấn (sai kiểu, sai giá trị enum).
     */
    @Bean
    OpenApiCustomizer validationErrorResponse() {
        return openApi -> openApi.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .filter(OpenApiConfig::acceptsClientInput)
                .forEach(operation -> operation.getResponses().addApiResponse("400", new ApiResponse()
                        .description("Dữ liệu không hợp lệ — lỗi từng trường nằm trong 'errors', "
                                + "lỗi tham số nằm trong 'detail'")
                        .content(new Content().addMediaType("application/problem+json",
                                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))))));
    }

    /** Header X-User-Id có ở mọi thao tác nên không tính; nó sai thì trả 401 chứ không phải 400. */
    private static boolean acceptsClientInput(Operation operation) {
        if (operation.getRequestBody() != null) {
            return true;
        }
        return operation.getParameters() != null && operation.getParameters().stream()
                .anyMatch(parameter -> !"header".equals(parameter.getIn()));
    }

    /** MVP chưa có đăng nhập: cho phép chọn chủ sở hữu qua header ngay trên Swagger UI. */
    @Bean
    OperationCustomizer currentUserHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(new HeaderParameter()
                .name("X-User-Id")
                .required(false)
                .description("Id chủ nông trại (bỏ trống = người dùng mặc định 1). Thay bằng JWT ở Phase 5.")
                .schema(new IntegerSchema().format("int64")));
    }
}
