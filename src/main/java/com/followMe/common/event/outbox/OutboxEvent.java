package com.followMe.common.event.outbox;

import com.followMe.common.event.BaseEvent;

public record OutboxEvent(BaseEvent event) {
	public String correlationId() {
		String cid = event.getCorrelationId();
		return cid != null ? cid : event.getEventId();
	}

}
