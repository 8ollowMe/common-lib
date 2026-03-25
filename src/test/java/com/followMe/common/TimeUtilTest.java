package com.followMe.common;

import com.followMe.common.util.TimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilTest {

    @Test
    void instantRoundTrip() {
        LocalDateTime ldt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
        Instant instant = TimeUtil.toInstant(ldt);
        LocalDateTime back = TimeUtil.toLocalDateTime(instant);
        assertThat(back).isEqualTo(ldt);
    }

    @Test
    void formatInstant() {
        Instant instant = TimeUtil.toInstant(LocalDateTime.of(2024, 1, 15, 9, 30, 0));
        String formatted = TimeUtil.format(instant, "yyyy-MM-dd");
        assertThat(formatted).isEqualTo("2024-01-15");
    }

    @Test
    void isBetween() {
        Instant now = Instant.parse("2024-06-01T00:00:00Z");
        Instant start = Instant.parse("2024-05-01T00:00:00Z");
        Instant end = Instant.parse("2024-07-01T00:00:00Z");
        assertThat(TimeUtil.isBetween(now, start, end)).isTrue();
    }
}