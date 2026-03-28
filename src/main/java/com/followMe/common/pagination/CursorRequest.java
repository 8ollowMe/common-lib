package com.followMe.common.pagination;

import lombok.Getter;

/**
 * 커서 기반 페이지 요청 DTO.
 *
 * <p>커서가 {@code null}이면 첫 페이지를 의미합니다.
 * 허용되지 않은 size 값은 {@link PageRequest#DEFAULT_SIZE}(10)으로 대체됩니다.
 *
 * <pre>{@code
 * @GetMapping("/feeds")
 * public ApiResponse<CursorResponse<FeedResponse>> list(CursorRequest cursorRequest) {
 *     return ApiResponse.success(feedService.list(cursorRequest));
 * }
 * }</pre>
 *
 * @see CursorResponse
 */
@Getter
public class CursorRequest {

    private final String cursor; // null이면 첫 페이지
    private final int size;

    private CursorRequest(String cursor, int size) {
        this.cursor = cursor;
        this.size = PageRequest.ALLOWED_SIZES.contains(size) ? size : PageRequest.DEFAULT_SIZE;
    }

    public static CursorRequest of(String cursor, int size) {
        return new CursorRequest(cursor, size);
    }

    public boolean isFirst() {
        return cursor == null;
    }
}