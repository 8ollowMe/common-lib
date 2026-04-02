package com.followMe.common.event.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxStatusUpdater {

	private final OutboxRepository outboxRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void update(UUID id, boolean success) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			if (success) outbox.complete();
			else outbox.fail();
			outboxRepository.saveAndFlush(outbox);
		});
	}

}
