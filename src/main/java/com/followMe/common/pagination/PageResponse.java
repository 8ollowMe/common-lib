package com.followMe.common.pagination;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 응답 DTO.
 *
 * <p>Spring Data 의 {@link Page} 를 JSON 직렬화에 적합한 형태로 래핑합니다.
 *
 * <pre>{@code
 * Page<UserEntity> page = userRepository.findAll(pageable);
 * PageResponse<UserResponse> response = PageResponse.of(page, UserResponse::from);
 * }</pre>
 *
 * @param <T> 콘텐츠 항목 타입
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int page;          // 0-based
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    private PageResponse(List<T> content, int page, int size,
                         long totalElements, int totalPages,
                         boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    // ── 팩토리 ────────────────────────────────────────────────────────────────

    /** Spring Data {@link Page} 를 그대로 변환. */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 엔티티 Page → DTO Page 로 변환 (매핑 함수 제공).
     *
     * @param page    Spring Data Page
     * @param mapper  엔티티 → DTO 변환 함수
     */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    /** Spring Data 의존 없이 직접 생성. */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }
}