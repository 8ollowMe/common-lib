package com.followMe.common.pagination;

import lombok.Getter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * 허용된 사이즈(10 / 30 / 50)만 받는 페이지 요청 DTO.
 *
 * <p>허용되지 않은 사이즈는 기본값 10으로 대체됩니다.
 *
 * <pre>{@code
 * // Controller
 * @GetMapping("/users")
 * public ApiResponse<PageResponse<UserResponse>> list(
 *         @RequestParam(defaultValue = "0") int page,
 *         @RequestParam(defaultValue = "10") int size) {
 *
 *     Pageable pageable = PageRequest.of(page, size).toPageable();
 *     return ApiResponse.success(PageResponse.of(userService.list(pageable)));
 * }
 * }</pre>
 */
@Getter
public class PageRequest {

    public static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    public static final int DEFAULT_SIZE = 10;

    private final int page;   // 0-based
    private final int size;   // 10 | 30 | 50

    private PageRequest(int page, int size) {
        this.page = Math.max(0, page);
        this.size = ALLOWED_SIZES.contains(size) ? size : DEFAULT_SIZE;
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }

    /** Spring Data {@link Pageable} 로 변환 (정렬 없음). */
    public Pageable toPageable() {
        return org.springframework.data.domain.PageRequest.of(page, size);
    }

    /** Spring Data {@link Pageable} 로 변환 (정렬 포함). */
    public Pageable toPageable(Sort sort) {
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }
}