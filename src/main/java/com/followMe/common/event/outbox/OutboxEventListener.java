package com.followMe.common.event.outbox;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.followMe.common.event.BaseEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventListener {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final OutboxStatusUpdater outboxStatusUpdater;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle(OutboxEvent outboxEvent){
		BaseEvent event = outboxEvent.event();

		String payload;
		try {
			payload = objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException e) {
			log.error(
					"Failed to serialize event for Outbox. Marking as FAILED. eventType={}, domainType={}, domainId={}",
					event.getEventType(),
					event.getDomainType(),
					event.getDomainId(),
					e
			);
			Outbox failedOutbox = outboxRepository.save(Outbox.builder()
					.correlationId(outboxEvent.correlationId())
					.domainType(event.getDomainType())
					.domainId(event.getDomainId())
					.eventType(event.getEventType())
					.payload(null)
					.build());
			outboxStatusUpdater.update(failedOutbox.getId(), false);
			return;
		}
		Outbox outbox = outboxRepository.save(Outbox.builder()
				.correlationId(outboxEvent.correlationId())
				.domainType(event.getDomainType())
				.domainId(event.getDomainId())
				.eventType(event.getEventType())
				.payload(payload)
				.build());
		UUID id = outbox.getId();
		kafkaTemplate.send(event.getEventType(), event.getDomainId(), payload)
				.whenComplete((result, ex) -> outboxStatusUpdater.update(id, ex == null));
	}
}