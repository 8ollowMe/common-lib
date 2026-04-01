package com.followMe.common.event.scheduler;


import com.followMe.common.event.outbox.Outbox;
import com.followMe.common.event.outbox.OutboxRepository;
import com.followMe.common.event.outbox.OutboxStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {

	private static final int MAX_RETRY = 3;

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	@Scheduled(fixedDelay = 10_000)
	@Transactional
	public void relay() {
		List<Outbox> targets =
				outboxRepository.findByStatusInAndRetryCountLessThan(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
						MAX_RETRY);

		if (targets.isEmpty()) return;

		log.info("[Outbox] 재전송 대상 {}건", targets.size());

		for (Outbox outbox : targets) {
			UUID id = outbox.getId();
			kafkaTemplate.send(outbox.getEventType(), outbox.getDomainId(), outbox.getPayload())
					.whenComplete((result, ex) -> updateStatus(id, ex == null));
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateStatus(UUID id, boolean success) {
		outboxRepository.findById(id)
				.ifPresent(outbox -> {
					if (success) {
						outbox.complete();
						log.info("[Outbox] 재전송 성공: {}", outbox.getCorrelationId());
					}
					else {
						outbox.fail();
						log.warn("[Outbox] 재전송 실패 ({}회): {}", outbox.getRetryCount(), outbox.getCorrelationId());
					}
					outboxRepository.saveAndFlush(outbox);
				});
	}
}