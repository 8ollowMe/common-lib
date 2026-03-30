package com.followMe.common.pagination;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 컨트롤러 파라미터로 선언된 {@link PageRequest}를 쿼리 파라미터에서 자동 바인딩.
 *
 * <p>{@code ?page=0&size=10} 형태로 요청하면 {@link PageRequest}로 변환됩니다.
 * 허용되지 않은 size 값은 {@link PageRequest#DEFAULT_SIZE}(10)으로 대체됩니다.
 *
 * <pre>{@code
 * @GetMapping("/items")
 * public ApiResponse<PageResponse<ItemResponse>> list(PageRequest pageRequest) {
 *     return ApiResponse.success(itemService.list(pageRequest.toPageable()));
 * }
 * }</pre>
 */
public class PageRequestArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PageRequest.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String pageParam = webRequest.getParameter("page");
        String sizeParam = webRequest.getParameter("size");

        int page = parseOrDefault(pageParam, 0);
        int size = parseOrDefault(sizeParam, PageRequest.DEFAULT_SIZE);

        return PageRequest.of(page, size);
    }

    private int parseOrDefault(String value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}