package com.followMe.common.autoconfigure;

import com.followMe.common.pagination.CursorRequestArgumentResolver;
import com.followMe.common.pagination.PageRequestArgumentResolver;
import com.followMe.common.util.MdcTaskDecorator;
import com.followMe.common.web.GlobalExceptionHandler;
import com.followMe.common.web.MdcLoggingFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 환경에서 공통 빈 자동 등록.
 *
 * <p>{@link GlobalExceptionHandler} 를 직접 정의한 서비스에서는 이 설정이 적용되지 않습니다.
 * <p>{@link PageRequestArgumentResolver} 는 항상 등록됩니다.
 * <p>{@link MdcLoggingFilter} 는 가장 높은 우선순위로 등록되어 모든 요청에 MDC 정보를 주입합니다.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(MdcLoggingFilter.class)
    public FilterRegistrationBean<MdcLoggingFilter> mdcLoggingFilter() {
        FilterRegistrationBean<MdcLoggingFilter> registration = new FilterRegistrationBean<>(new MdcLoggingFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(MdcTaskDecorator.class)
    public MdcTaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PageRequestArgumentResolver());
        resolvers.add(new CursorRequestArgumentResolver());
    }
}