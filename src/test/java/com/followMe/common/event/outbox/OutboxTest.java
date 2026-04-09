package com.followMe.common.event.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxTest {

    @Test
    void 기본_상태는_PENDING() {
        Outbox outbox = Outbox.builder()
                .correlationId("corr-1")
                .domainType("ORDER")
                .domainId("order-1")
                .eventType("OrderCreatedEvent")
                .payload("{\"amount\":1000}")
                .build();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outbox.getRetryCount()).isZero();
    }

    @Test
    void complete_호출시_상태가_PROCESSED로_변경된다() {
        Outbox outbox = Outbox.builder()
                .correlationId("corr-1")
                .domainType("ORDER")
                .domainId("order-1")
                .eventType("OrderCreatedEvent")
                .payload("{\"amount\":1000}")
                .build();

        outbox.complete();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
    }

    @Test
    void fail_호출시_상태가_FAILED로_변경되고_retryCount가_증가한다() {
        Outbox outbox = Outbox.builder()
                .correlationId("corr-1")
                .domainType("ORDER")
                .domainId("order-1")
                .eventType("OrderCreatedEvent")
                .payload("{\"amount\":1000}")
                .build();

        outbox.fail();

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
    }

    @Test
    void fail_여러번_호출시_retryCount가_누적된다() {
        Outbox outbox = Outbox.builder()
                .correlationId("corr-1")
                .domainType("ORDER")
                .domainId("order-1")
                .eventType("OrderCreatedEvent")
                .payload("{\"amount\":1000}")
                .build();

        outbox.fail();
        outbox.fail();
        outbox.fail();

        assertThat(outbox.getRetryCount()).isEqualTo(3);
    }
}