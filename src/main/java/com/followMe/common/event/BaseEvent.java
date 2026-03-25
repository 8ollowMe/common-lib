package com.followMe.common.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka 도메인 이벤트 기반 클래스.
 *
 * <p>모든 이벤트는 이 클래스를 상속합니다.
 *
 * <pre>{@code
 * public class OrderCreatedEvent extends BaseEvent {
 *     private final Long orderId;
 *     private final String userId;
 *
 *     public OrderCreatedEvent(Long orderId, String userId) {
 *         super();  // eventId, eventType, occurredAt 자동 설정
 *         this.orderId = orderId;
 *         this.userId = userId;
 *     }
 * }
 * }</pre>
 */
@Getter
public abstract class BaseEvent {

    /** 이벤트 고유 ID (UUID) */
    private final String eventId;

    /** 이벤트 타입 (클래스 심플 네임) */
    private final String eventType;

    /** 이벤트 발생 시각 (UTC) */
    private final Instant occurredAt;

    /** 분산 추적용 Correlation ID (MDC 에서 자동 주입하거나 직접 설정) */
    private String correlationId;

    /** 이벤트 스키마 버전 (소비자 하위 호환성 관리용) */
    private final int version;

    protected BaseEvent() {
        this(1);
    }

    protected BaseEvent(int version) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = this.getClass().getSimpleName();
        this.occurredAt = Instant.now();
        this.version = version;
    }

    /** Correlation ID 를 직접 설정할 경우 사용 (MDC 연동 등) */
    public BaseEvent withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
}