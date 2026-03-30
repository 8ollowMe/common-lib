package com.followMe.common.pagination;

import lombok.Getter;

import java.util.List;
import java.util.function.Function;

/**
 * 커서 기반 페이지 응답 DTO.
 *
 * <p>{@code nextCursor}가 {@code null}이면 마지막 페이지입니다.
 *
 * <pre>{@code
 * List<FeedEntity> feeds = feedRepository.findByCursor(cursor, size + 1);
 * return CursorResponse.of(feeds, size, FeedResponse::from, FeedEntity::getCursorId);
 * }</pre>
 *
 * @param <T> 콘텐츠 항목 타입
 */
@Getter
public class CursorResponse<T> {

    private final List<T> content;
    private final String nextCursor; // null이면 마지막 페이지
    private final boolean hasNext;

    private CursorResponse(List<T> content, String nextCursor) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasNext = nextCursor != null;
    }

    // ── 팩토리 ────────────────────────────────────────────────────────────────

    /**
     * 엔티티 리스트 → DTO 리스트로 변환하며 다음 커서를 추출.
     *
     * <p>조회 시 {@code size + 1}개를 가져와서 다음 페이지 존재 여부를 판단합니다.
     *
     * @param entities     조회된 엔티티 목록 (size + 1개 조회 권장)
     * @param size         요청 사이즈
     * @param mapper       엔티티 → DTO 변환 함수
     * @param cursorExtractor 다음 커서 값 추출 함수 (마지막 엔티티 기준)
     */
    public static <E, T> CursorResponse<T> of(List<E> entities, int size,
                                               Function<E, T> mapper,
                                               Function<E, String> cursorExtractor) {
        boolean hasNext = entities.size() > size;
        List<E> sliced = hasNext ? entities.subList(0, size) : entities;
        String nextCursor = hasNext ? cursorExtractor.apply(sliced.get(sliced.size() - 1)) : null;
        return new CursorResponse<>(sliced.stream().map(mapper).toList(), nextCursor);
    }

    /** 이미 변환된 DTO 리스트로 직접 생성. */
    public static <T> CursorResponse<T> of(List<T> content, String nextCursor) {
        return new CursorResponse<>(content, nextCursor);
    }
}