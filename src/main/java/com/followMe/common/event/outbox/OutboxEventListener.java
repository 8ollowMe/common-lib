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

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handle(OutboxEvent outboxEvent) throws JsonProcessingException {
		BaseEvent event = outboxEvent.event();

		Outbox outbox = outboxRepository.save(Outbox.builder()
				.correlationId(outboxEvent.correlationId())
				.domainType(event.getDomainType())
				.domainId(event.getDomainId())
				.eventType(event.getEventType())
				.payload(objectMapper.writeValueAsString(event))
				.build());

		UUID id = outbox.getId();
		kafkaTemplate.send(event.getEventType(), event.getDomainId(), event)
				.whenComplete((result, ex) -> updateStatus(id, ex == null));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateStatus(UUID id, boolean success) {
		outboxRepository.findById(id)
				.ifPresent(outbox -> {
					if (success) outbox.complete();
					else outbox.fail();
					outboxRepository.saveAndFlush(outbox);
				});
	}
}