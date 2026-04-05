package com.followMe.common.event.scheduler;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.followMe.common.event.outbox.Outbox;
import com.followMe.common.event.outbox.OutboxRepository;
import com.followMe.common.event.outbox.OutboxStatus;
import com.followMe.common.event.outbox.OutboxStatusUpdater;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {

	private static final int MAX_RETRY = 3;

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final OutboxStatusUpdater outboxStatusUpdater;
	private final ObjectMapper objectMapper;

	@Scheduled(fixedDelay = 10_000)
	@Transactional
	public void relay() {
		List<Outbox> targets =
				outboxRepository.findByStatusInAndRetryCountLessThan(List.of(OutboxStatus.FAILED),
						MAX_RETRY);

		if (targets.isEmpty()) return;

		log.info("[Outbox] 재전송 대상 {}건", targets.size());

		for (Outbox outbox : targets) {
			UUID id = outbox.getId();
			try {
				Object payload = objectMapper.readValue(outbox.getPayload(), Object.class);
				kafkaTemplate.send(outbox.getEventType(), outbox.getDomainId(), payload)
						.whenComplete((result, ex) -> outboxStatusUpdater.update(id, ex == null));
			} catch (JsonProcessingException e) {
				log.error("[Outbox] 재전송 실패 - payload 파싱 오류. outboxId={}", id, e);
				outboxStatusUpdater.update(id, false);
			}
		}
	}

}