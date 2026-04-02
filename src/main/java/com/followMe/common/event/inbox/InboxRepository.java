package com.followMe.common.event.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InboxRepository extends JpaRepository<Inbox, UUID> {
	boolean existsByIdAndMessageGroup(UUID id, String messageGroup);
}