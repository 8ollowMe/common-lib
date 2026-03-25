package com.followMe.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * {@link Instant} 기반 시간 유틸리티.
 *
 * <p>DB/도메인 계층은 {@code Instant}(UTC), 표현 계층은 서비스 TimeZone으로 변환하는 패턴을 권장합니다.
 */
public final class TimeUtil {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    public static final ZoneId UTC = ZoneOffset.UTC;

    private TimeUtil() {}

    // ── Instant ↔ LocalDateTime ───────────────────────────────────────────────

    /** Instant → 서울 시간대 LocalDateTime */
    public static LocalDateTime toLocalDateTime(Instant instant) {
        return toLocalDateTime(instant, DEFAULT_ZONE);
    }

    /** Instant → 지정 시간대 LocalDateTime */
    public static LocalDateTime toLocalDateTime(Instant instant, ZoneId zone) {
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, zone);
    }

    /** 서울 시간대 LocalDateTime → Instant */
    public static Instant toInstant(LocalDateTime localDateTime) {
        return toInstant(localDateTime, DEFAULT_ZONE);
    }

    /** 지정 시간대 LocalDateTime → Instant */
    public static Instant toInstant(LocalDateTime localDateTime, ZoneId zone) {
        if (localDateTime == null) return null;
        return localDateTime.atZone(zone).toInstant();
    }

    // ── 포맷팅 ────────────────────────────────────────────────────────────────

    /** Instant → "yyyy-MM-dd HH:mm:ss" (서울 시간대) */
    public static String format(Instant instant) {
        return format(instant, "yyyy-MM-dd HH:mm:ss", DEFAULT_ZONE);
    }

    /** Instant → 지정 패턴 문자열 (서울 시간대) */
    public static String format(Instant instant, String pattern) {
        return format(instant, pattern, DEFAULT_ZONE);
    }

    /** Instant → 지정 패턴 문자열 (지정 시간대) */
    public static String format(Instant instant, String pattern, ZoneId zone) {
        if (instant == null) return null;
        return DateTimeFormatter.ofPattern(pattern)
                .withZone(zone)
                .format(instant);
    }

    // ── 비교 유틸 ─────────────────────────────────────────────────────────────

    /** a 가 b 보다 이전인지 */
    public static boolean isBefore(Instant a, Instant b) {
        return a != null && b != null && a.isBefore(b);
    }

    /** a 가 b 보다 이후인지 */
    public static boolean isAfter(Instant a, Instant b) {
        return a != null && b != null && a.isAfter(b);
    }

    /** now 가 start ~ end 범위 안에 있는지 */
    public static boolean isBetween(Instant now, Instant start, Instant end) {
        return !now.isBefore(start) && !now.isAfter(end);
    }
}