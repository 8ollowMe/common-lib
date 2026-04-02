package com.followMe.common.autoconfigure;

import com.followMe.common.pagination.CursorRequest;
import com.followMe.common.pagination.PageRequest;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.HandlerMethod;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * springdoc-openapi 가 클래스패스에 있을 때 공통 Swagger 설정을 자동 등록합니다.
 *
 * <ul>
 *   <li>Bearer JWT 인증 스키마 — {@code components/securitySchemes/bearerAuth} 로 등록.
 *       엔드포인트에 적용하려면 서비스에서 {@code @SecurityRequirement(name = "bearerAuth")} 를 사용하세요.</li>
 *   <li>{@link PageRequest} 파라미터 → {@code page}, {@code size} 쿼리 파라미터로 문서화</li>
 *   <li>{@link CursorRequest} 파라미터 → {@code cursor}, {@code size} 쿼리 파라미터로 문서화</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(OpenAPI.class)
public class CommonSwaggerAutoConfiguration {

    static {
        // springdoc 이 PageRequest / CursorRequest 를 자동 분석하지 않도록 등록.
        // OperationCustomizer 에서 직접 올바른 파라미터를 추가합니다.
        SpringDocUtils.getConfig()
                .addRequestWrapperToIgnore(PageRequest.class)
                .addRequestWrapperToIgnore(CursorRequest.class);
    }

    /**
     * Bearer 인증 스키마를 OpenAPI components 에 추가합니다.
     * 소비자 서비스에서 이미 "bearerAuth" 스키마를 정의한 경우 덮어쓰지 않습니다.
     */
    @Bean
    public OpenApiCustomizer bearerAuthCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            components.addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Bearer 토큰. 로그인 후 발급받은 accessToken 을 입력하세요."));
        };
    }

    /**
     * {@link PageRequest} / {@link CursorRequest} 를 Swagger 에서 올바른 쿼리 파라미터로 표시합니다.
     */
    @Bean
    public OperationCustomizer paginationParameterCustomizer() {
        return (operation, handlerMethod) -> {
            boolean hasPageRequest = hasParamType(handlerMethod, PageRequest.class);
            boolean hasCursorRequest = hasParamType(handlerMethod, CursorRequest.class);

            if (!hasPageRequest && !hasCursorRequest) {
                return operation;
            }

            List<Parameter> params = operation.getParameters() != null
                    ? new ArrayList<>(operation.getParameters())
                    : new ArrayList<>();

            if (hasPageRequest) {
                addIfAbsent(params, pageParam());
                addIfAbsent(params, sizeParam());
            }
            if (hasCursorRequest) {
                addIfAbsent(params, cursorParam());
                addIfAbsent(params, sizeParam());
            }

            operation.setParameters(params);
            return operation;
        };
    }

    private boolean hasParamType(HandlerMethod handlerMethod, Class<?> type) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(p -> p.getParameterType().equals(type));
    }

    private void addIfAbsent(List<Parameter> params, Parameter newParam) {
        boolean exists = params.stream().anyMatch(p -> newParam.getName().equals(p.getName()));
        if (!exists) {
            params.add(newParam);
        }
    }

    private Parameter pageParam() {
        return new Parameter()
                .name("page")
                .in("query")
                .description("페이지 번호 (0부터 시작, 기본값: 0)")
                .required(false)
                .schema(new IntegerSchema().minimum(BigDecimal.ZERO).example(0));
    }

    private Parameter sizeParam() {
        return new Parameter()
                .name("size")
                .in("query")
                .description("페이지 크기 (허용값: 10 / 30 / 50, 기본값: 10)")
                .required(false)
                .schema(new IntegerSchema()
                        ._enum(List.of(10, 30, 50))
                        .example(10));
    }

    private Parameter cursorParam() {
        return new Parameter()
                .name("cursor")
                .in("query")
                .description("커서 값 (생략하거나 null 이면 첫 페이지)")
                .required(false)
                .schema(new StringSchema());
    }
}
