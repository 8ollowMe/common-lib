package com.followMe.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.followMe.common.event.Events;
import com.followMe.common.event.outbox.OutboxEventListener;
import com.followMe.common.event.outbox.OutboxRepository;
import com.followMe.common.event.scheduler.OutboxRelayScheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
public class CommonEventAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public Events events(ApplicationEventPublisher publisher) {
    return new Events(publisher);
  }

  @Bean
  @ConditionalOnMissingBean
  public OutboxEventListener outboxEventListener(
      OutboxRepository outboxRepository,
      KafkaTemplate<String, Object> kafkaTemplate,
      ObjectMapper objectMapper) {
    return new OutboxEventListener(outboxRepository, kafkaTemplate, objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public OutboxRelayScheduler outboxRelayScheduler(
      OutboxRepository outboxRepository, KafkaTemplate<String, Object> kafkaTemplate) {
    return new OutboxRelayScheduler(outboxRepository, kafkaTemplate);
  }
}
