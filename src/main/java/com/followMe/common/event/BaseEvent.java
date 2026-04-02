package com.followMe.common.event;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

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

  /** 도메인 타입 (Outbox 저장 및 파티션 키 계산용) */
  private final String domainType;

  /** 도메인 ID (Outbox 저장 및 파티션 키 계산용) */
  private final String domainId;

  private final Object payload;

  /** 이벤트 발생 시각 (UTC) */
  private final Instant occurredAt;

  /** 분산 추적용 Correlation ID (MDC 에서 자동 주입하거나 직접 설정) */
  private String correlationId;

  protected BaseEvent(String domainType, UUID domainId, Object payload) {
    this(domainType, domainId == null ? null : domainId.toString(), payload);
  }

  protected BaseEvent(String domainType, Object payload) {
    this(domainType, (String) null, payload);
  }

  protected BaseEvent(String domainType, String domainId, Object payload) {
    this.domainType = domainType;
    this.domainId = domainId;
	  this.payload = payload;
	  this.eventId = UUID.randomUUID().toString();
    this.eventType = this.getClass().getSimpleName();
    this.occurredAt = Instant.now();
  }

  /** Correlation ID 를 직접 설정할 경우 사용 (MDC 연동 등) */
  public BaseEvent withCorrelationId(String correlationId) {
    this.correlationId = correlationId;
    return this;
  }
}
