package com.followMe.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청마다 MDC 에 추적 정보를 주입하는 필터.
 *
 * <ul>
 *   <li>{@code traceId} — {@code X-Trace-Id} 헤더가 있으면 그 값을 사용, 없으면 UUID 앞 8자리 생성</li>
 *   <li>{@code userId}  — {@code X-User-Id} 헤더 값 (API Gateway/인증 서비스가 주입한 값)</li>
 *   <li>{@code method}  — HTTP 메서드 (GET, POST …)</li>
 *   <li>{@code uri}     — 요청 URI</li>
 * </ul>
 *
 * <p>응답 헤더에 {@code X-Trace-Id} 를 다시 실어 클라이언트가 요청을 추적할 수 있게 합니다.
 * MDC 는 요청 처리 완료 후 반드시 초기화됩니다.
 */
public class MdcLoggingFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String USER_ID_HEADER  = "X-User-Id";

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USER_ID  = "userId";
    public static final String MDC_METHOD   = "method";
    public static final String MDC_URI      = "uri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String traceId = resolveTraceId(request);
            String userId  = request.getHeader(USER_ID_HEADER);

            MDC.put(MDC_TRACE_ID, traceId);
            MDC.put(MDC_METHOD, request.getMethod());
            MDC.put(MDC_URI, request.getRequestURI());
            if (userId != null && !userId.isBlank()) {
                MDC.put(MDC_USER_ID, userId);
            }

            response.setHeader(TRACE_ID_HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String header = request.getHeader(TRACE_ID_HEADER);
        return (header != null && !header.isBlank())
                ? header
                : UUID.randomUUID().toString().substring(0, 8);
    }
}
