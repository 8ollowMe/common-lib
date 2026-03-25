package com.followMe.common.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka 이벤트 발행 유틸.
 *
 * <p>Auto-configuration 으로 자동 등록됩니다 ({@code spring-kafka} 의존성이 있을 때만).
 *
 * <p>소비자 서비스 application.yml 에서 Kafka 직렬화를 설정하세요:
 * <pre>{@code
 * spring:
 *   kafka:
 *     producer:
 *       key-serializer: org.apache.kafka.common.serialization.StringSerializer
 *       value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
 *       properties:
 *         spring.json.add.type.headers: false
 * }</pre>
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * 이벤트 ID 를 Kafka 파티션 키로 사용해 발행.
     */
    public <E extends BaseEvent> void publish(String topic, E event) {
        publish(topic, event.getEventId(), event);
    }

    /**
     * 지정 키로 발행.
     */
    public <E extends BaseEvent> void publish(String topic, String key, E event) {
        log.debug("[Kafka] publish topic={}, key={}, eventType={}, eventId={}",
                topic, key, event.getEventType(), event.getEventId());
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] publish failed topic={}, eventId={}", topic, event.getEventId(), ex);
                    } else {
                        log.debug("[Kafka] publish success topic={}, partition={}, offset={}",
                                topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * 비동기 발행 — 결과를 직접 처리할 때.
     */
    public <E extends BaseEvent> CompletableFuture<SendResult<String, Object>> publishAsync(
            String topic, E event) {
        return publishAsync(topic, event.getEventId(), event);
    }

    /**
     * 비동기 발행 (지정 키) — 결과를 직접 처리할 때.
     */
    public <E extends BaseEvent> CompletableFuture<SendResult<String, Object>> publishAsync(
            String topic, String key, E event) {
        log.debug("[Kafka] publishAsync topic={}, key={}, eventType={}", topic, key, event.getEventType());
        return kafkaTemplate.send(topic, key, event);
    }
}