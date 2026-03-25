package com.followMe.common;

import com.followMe.common.pagination.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void springPageToPageResponse() {
        var page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 10), 2);
        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.getContent()).containsExactly("a", "b");
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.isFirst()).isTrue();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void mapperConversion() {
        var page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(1, 3), 9);
        PageResponse<String> response = PageResponse.of(page, String::valueOf);

        assertThat(response.getContent()).containsExactly("1", "2", "3");
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(3);
        assertThat(response.isFirst()).isFalse();
        assertThat(response.isLast()).isFalse();
    }
}