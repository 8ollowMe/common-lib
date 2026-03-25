package com.followMe.common.response;

import com.followMe.common.exception.ErrorCode;
import com.followMe.common.pagination.PageResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.function.Function;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse {

    private final boolean success;
    private final Object data;
    private final ErrorResponse error;

    // ── ResponseEntity 반환 (컨트롤러에서 바로 return) ────────────────────────

    public static ResponseEntity<ApiResponse> ok(Object data) {
        return ResponseEntity.ok(new ApiResponse(true, data, null));
    }

    public static ResponseEntity<ApiResponse> ok() {
        return ResponseEntity.ok(new ApiResponse(true, null, null));
    }

    public static ResponseEntity<ApiResponse> created(Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse(true, data, null));
    }

    public static ResponseEntity<ApiResponse> created() {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse(true, null, null));
    }

    public static <T> ResponseEntity<ApiResponse> ok(Page<T> page) {
        return ResponseEntity.ok(new ApiResponse(true, PageResponse.of(page), null));
    }

    public static <E, T> ResponseEntity<ApiResponse> ok(Page<E> page, Function<E, T> mapper) {
        return ResponseEntity.ok(new ApiResponse(true, PageResponse.of(page, mapper), null));
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(true, data, null);
    }

    public static ApiResponse success() {
        return new ApiResponse(true, null, null);
    }

    public static <T> ApiResponse page(Page<T> page) {
        return new ApiResponse(true, PageResponse.of(page), null);
    }

    public static <E, T> ApiResponse page(Page<E> page, Function<E, T> mapper) {
        return new ApiResponse(true, PageResponse.of(page, mapper), null);
    }

    public static ApiResponse error(ErrorCode errorCode) {
        return new ApiResponse(false, null,
                ErrorResponse.of(errorCode.getCode(), errorCode.getMessage()));
    }

    public static ApiResponse error(ErrorCode errorCode, String detailMessage) {
        return new ApiResponse(false, null,
                ErrorResponse.of(errorCode.getCode(), detailMessage));
    }

    public static ApiResponse error(ErrorCode errorCode, List<ErrorResponse.FieldError> fieldErrors) {
        return new ApiResponse(false, null,
                ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), fieldErrors));
    }
}