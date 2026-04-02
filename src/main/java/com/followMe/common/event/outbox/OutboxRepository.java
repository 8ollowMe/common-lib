package com.followMe.common.event.outbox;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
  List<Outbox> findByStatusInAndRetryCountLessThan(List<OutboxStatus> statuses, int maxRetry);

  Optional<Outbox> findByCorrelationId(String correlationId);
}
