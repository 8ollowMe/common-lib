package com.followMe.common.event.scheduler;


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
			kafkaTemplate.send(outbox.getEventType(), outbox.getDomainId(), outbox.getPayload())
					.whenComplete((result, ex) -> outboxStatusUpdater.update(id, ex == null));
		}
	}

}