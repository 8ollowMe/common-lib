package com.followMe.common.event.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.followMe.common.event.BaseEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class OutboxEventListenerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OutboxStatusUpdater outboxStatusUpdater;

    @InjectMocks
    private OutboxEventListener listener;

    // ── 테스트용 이벤트 ──────────────────────────────────────────────────────

    static class OrderCreatedEvent extends BaseEvent {
        OrderCreatedEvent() {
            super("ORDER", "order-1", Map.of("amount", 1000));
        }
    }

    // ── 성공 시나리오 ─────────────────────────────────────────────────────────

    @Test
    void 직렬화_성공_Kafka_발행_성공시_PROCESSED로_업데이트() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(event);
        String expectedPayload = "{\"amount\":1000}";
        UUID savedId = UUID.randomUUID();
        Outbox savedOutbox = buildOutbox(savedId, event, expectedPayload);

        given(objectMapper.writeValueAsString(event)).willReturn(expectedPayload);
        given(outboxRepository.save(any())).willReturn(savedOutbox);
        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        listener.handle(outboxEvent);

        // DB에 JSON 문자열로 저장됐는지
        verify(outboxRepository).save(argThat(o -> expectedPayload.equals(o.getPayload())));

        // ★ Kafka에는 event object 그대로 전달 — String으로 직렬화하면 이중 직렬화 발생
        verify(kafkaTemplate).send(event.getEventType(), event.getDomainId(), event);

        verify(outboxStatusUpdater).update(savedId, true);
    }

    @Test
    void correlationId가_설정된_경우_이벤트ID_대신_correlationId_사용() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.withCorrelationId("my-corr-id");
        OutboxEvent outboxEvent = new OutboxEvent(event);
        UUID savedId = UUID.randomUUID();
        Outbox savedOutbox = buildOutbox(savedId, event, "{}");

        given(objectMapper.writeValueAsString(event)).willReturn("{}");
        given(outboxRepository.save(any())).willReturn(savedOutbox);
        given(kafkaTemplate.send(anyString(), anyString(), any()))
                .willReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        listener.handle(outboxEvent);

        // correlationId()가 "my-corr-id"를 반환하는지 간접 검증 (save 인자의 correlationId)
        verify(outboxRepository).save(argThat(o -> "my-corr-id".equals(o.getCorrelationId())));
    }

    // ── Kafka 발행 실패 ───────────────────────────────────────────────────────

    @Test
    void Kafka_발행_실패시_상태가_FAILED로_업데이트된다() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(event);
        UUID savedId = UUID.randomUUID();
        Outbox savedOutbox = buildOutbox(savedId, event, "{}");

        CompletableFuture<SendResult<String, Object>> failedFuture =
                CompletableFuture.failedFuture(new RuntimeException("Kafka unavailable"));

        given(objectMapper.writeValueAsString(event)).willReturn("{}");
        given(outboxRepository.save(any())).willReturn(savedOutbox);
        given(kafkaTemplate.send(anyString(), anyString(), any())).willReturn(failedFuture);

        listener.handle(outboxEvent);

        verify(outboxStatusUpdater).update(savedId, false);
    }

    // ── 직렬화 실패 ───────────────────────────────────────────────────────────

    @Test
    void 직렬화_실패시_payload_null로_저장하고_Kafka_발행하지_않는다() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(event);
        UUID savedId = UUID.randomUUID();
        Outbox failedOutbox = buildOutbox(savedId, event, null);

        given(objectMapper.writeValueAsString(event))
                .willThrow(new JsonProcessingException("serialize error") {});
        given(outboxRepository.save(any())).willReturn(failedOutbox);

        listener.handle(outboxEvent);

        // payload가 null인 채로 저장
        verify(outboxRepository).save(argThat(o -> o.getPayload() == null));

        // Kafka 발행 없이 바로 FAILED 처리
        verifyNoInteractions(kafkaTemplate);
        verify(outboxStatusUpdater).update(savedId, false);
    }

    @Test
    void 직렬화_실패시_Outbox에_올바른_메타데이터_저장된다() throws Exception {
        OrderCreatedEvent event = new OrderCreatedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(event);
        Outbox failedOutbox = buildOutbox(UUID.randomUUID(), event, null);

        given(objectMapper.writeValueAsString(event))
                .willThrow(new JsonProcessingException("serialize error") {});
        given(outboxRepository.save(any())).willReturn(failedOutbox);

        listener.handle(outboxEvent);

        verify(outboxRepository).save(argThat(o ->
                "ORDER".equals(o.getDomainType()) &&
                "order-1".equals(o.getDomainId()) &&
                "OrderCreatedEvent".equals(o.getEventType())
        ));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Outbox buildOutbox(UUID id, BaseEvent event, String payload) {
        Outbox outbox = Outbox.builder()
                .correlationId(event.getCorrelationId() != null ? event.getCorrelationId() : event.getEventId())
                .domainType(event.getDomainType())
                .domainId(event.getDomainId())
                .eventType(event.getEventType())
                .payload(payload)
                .build();
        // JPA가 persist 후 주입하는 id를 단위 테스트에서 직접 설정
        ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }
}