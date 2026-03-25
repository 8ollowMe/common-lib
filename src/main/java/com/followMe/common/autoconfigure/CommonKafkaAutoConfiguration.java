package com.followMe.common.autoconfigure;

import com.followMe.common.event.KafkaEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * spring-kafka 의존성이 있는 서비스에서만 {@link KafkaEventPublisher} 자동 등록.
 *
 * <p>Kafka 를 사용하지 않는 서비스에서는 이 설정이 활성화되지 않습니다.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
public class CommonKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(KafkaEventPublisher.class)
    public KafkaEventPublisher kafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventPublisher(kafkaTemplate);
    }
}