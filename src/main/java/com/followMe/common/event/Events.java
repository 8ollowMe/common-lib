package com.followMe.common.event;

import com.followMe.common.event.exception.EventPublishFailureEvent;
import com.followMe.common.event.outbox.OutboxEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Events {
	private static ApplicationEventPublisher publisher;
	private final ApplicationEventPublisher eventPublisher;

	@PostConstruct
	void init() {
		Events.publisher = this.eventPublisher;
	}

	public static void trigger(BaseEvent event) {
		if (publisher == null) {
			throw new EventPublishFailureEvent("ApplicationEventPublisher is not initialized yet.");
		}
		publisher.publishEvent(new OutboxEvent(event));
	}
}
