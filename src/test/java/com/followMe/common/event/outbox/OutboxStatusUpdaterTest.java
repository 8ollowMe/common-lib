package com.followMe.common.event.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class OutboxStatusUpdaterTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxStatusUpdater updater;

    @Test
    void update_성공_true시_outbox_상태가_PROCESSED로_변경된다() {
        UUID id = UUID.randomUUID();
        Outbox outbox = buildOutbox(id);
        given(outboxRepository.findById(id)).willReturn(Optional.of(outbox));

        updater.update(id, true);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSED);
        verify(outboxRepository).saveAndFlush(outbox);
    }

    @Test
    void update_실패_false시_outbox_상태가_FAILED로_변경되고_retryCount가_증가한다() {
        UUID id = UUID.randomUUID();
        Outbox outbox = buildOutbox(id);
        given(outboxRepository.findById(id)).willReturn(Optional.of(outbox));

        updater.update(id, false);

        assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outbox.getRetryCount()).isEqualTo(1);
        verify(outboxRepository).saveAndFlush(outbox);
    }

    @Test
    void update_outbox가_없으면_아무것도_하지_않는다() {
        UUID id = UUID.randomUUID();
        given(outboxRepository.findById(id)).willReturn(Optional.empty());

        updater.update(id, true);

        verify(outboxRepository, never()).saveAndFlush(any());
    }

    private Outbox buildOutbox(UUID id) {
        return Outbox.builder()
                .correlationId("corr-" + id)
                .domainType("ORDER")
                .domainId("order-1")
                .eventType("OrderCreatedEvent")
                .payload("{\"amount\":1000}")
                .build();
    }
}