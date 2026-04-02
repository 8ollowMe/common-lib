package com.followMe.common.util;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 비동기 스레드에 MDC 컨텍스트를 전파하는 {@link TaskDecorator}.
 *
 * <p>MDC 는 내부적으로 {@link ThreadLocal} 을 사용하기 때문에, {@code @Async} 메서드나
 * {@link java.util.concurrent.CompletableFuture} 처럼 별도 스레드에서 실행되는 작업에서는
 * 부모 스레드의 {@code traceId}, {@code userId} 등 MDC 값이 사라집니다.
 *
 * <p>이 데코레이터를 {@code ThreadPoolTaskExecutor} 에 등록하면 부모 스레드의 MDC 를
 * 자식 스레드로 복사해 ELK 로그 추적이 끊기지 않습니다.
 *
 * <pre>{@code
 * // AsyncConfig.java (각 서비스에서 설정)
 * @Bean
 * public Executor asyncExecutor(MdcTaskDecorator mdcTaskDecorator) {
 *     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *     executor.setTaskDecorator(mdcTaskDecorator);
 *     executor.initialize();
 *     return executor;
 * }
 * }</pre>
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}