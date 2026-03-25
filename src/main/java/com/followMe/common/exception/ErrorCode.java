package com.followMe.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 인터페이스.
 *
 * <p>각 서비스는 이 인터페이스를 구현하는 enum 을 정의합니다.
 *
 * <pre>{@code
 * public enum UserErrorCode implements ErrorCode {
 *     USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
 *     DUPLICATE_EMAIL("U002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
 *
 *     private final String code;
 *     private final String message;
 *     private final HttpStatus httpStatus;
 *     // constructor, getters ...
 * }
 * }</pre>
 */
public interface ErrorCode {

    /** 서비스 고유 에러 코드 (예: "U001", "O002") */
    String getCode();

    /** 사용자에게 노출할 메시지 */
    String getMessage();

    /** HTTP 상태 코드 */
    HttpStatus getHttpStatus();
}