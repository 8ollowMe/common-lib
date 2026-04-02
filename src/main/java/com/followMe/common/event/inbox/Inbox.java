package com.followMe.common.event.inbox;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Builder
@Table(name = "p_inbox", indexes = {
		@Index(name = "idx_inbox_message_group", columnList = "messageGroup"),
		@Index(name = "idx_inbox_processed_at", columnList = "processedAt")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public class Inbox {

	@Id
	@Column(columnDefinition = "uuid")
	private UUID id; // = 생산자의 eventId

	@Column(length = 100)
	private String messageGroup; // topic 또는 consumer group

	@CreatedDate
	@Column(updatable = false)
	private Instant processedAt;
}
