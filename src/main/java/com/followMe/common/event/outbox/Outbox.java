package com.followMe.common.event.outbox;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(name = "p_outbox", indexes = @Index(name = "idx_outbox_status", columnList = "status"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public class Outbox {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(columnDefinition = "uuid")
	private UUID id;

	@Column(length = 64, nullable = false, unique = true)
	private String correlationId;

	@Column(length = 50, nullable = false)
	private String domainType;

	@Column(length = 50, nullable = false)
	private String domainId;

	@Column(length = 100, nullable = false)
	private String eventType;

	@Column(columnDefinition = "text")
	private String payload;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private OutboxStatus status = OutboxStatus.PENDING;

	@Builder.Default
	private int retryCount = 0;

	@CreatedDate
	@Column(updatable = false)
	private Instant createdAt;

	public void complete() {
		this.status = OutboxStatus.PROCESSED;
	}

	public void fail() {
		this.status = OutboxStatus.FAILED;
		this.retryCount++;
	}

}
