package com.followMe.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 공통 에러 코드.
 *
 * <p>접두어 {@code C} = Common
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 4xx
    INVALID_INPUT("C001", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("C002", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("C003", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("C004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CONFLICT("C005", "요청이 현재 상태와 충돌합니다.", HttpStatus.CONFLICT),
    UNSUPPORTED_MEDIA_TYPE("C006", "지원하지 않는 미디어 타입입니다.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    TOO_MANY_REQUESTS("C007", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS),

    // 5xx
    INTERNAL_SERVER_ERROR("C500", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("C503", "서비스를 일시적으로 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    EVENT_PUBLISH_FAILURE("C510", "이벤트 발행에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR)

    ;


    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}