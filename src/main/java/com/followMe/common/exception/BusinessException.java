package com.followMe.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 예외.
 *
 * <p>각 서비스는 이 클래스를 직접 사용하거나 도메인별로 상속하여 사용합니다.
 *
 * <pre>{@code
 * // 직접 사용
 * throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
 *
 * // 커스텀 메시지
 * throw new BusinessException(UserErrorCode.USER_NOT_FOUND, "ID: " + userId + " 사용자 없음");
 *
 * // 서비스별 상속
 * public class UserNotFoundException extends BusinessException {
 *     public UserNotFoundException(Long userId) {
 *         super(UserErrorCode.USER_NOT_FOUND, "userId=" + userId);
 *     }
 * }
 * }</pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}