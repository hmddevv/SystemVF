package com.hmdao.farm.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
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

    /** MVP chưa có đăng nhập: cho phép chọn chủ sở hữu qua header ngay trên Swagger UI. */
    @Bean
    OperationCustomizer currentUserHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(new HeaderParameter()
                .name("X-User-Id")
                .required(false)
                .description("Id chủ nông trại (bỏ trống = người dùng mặc định 1). Thay bằng JWT ở Phase 5.")
                .schema(new IntegerSchema()));
    }
}
