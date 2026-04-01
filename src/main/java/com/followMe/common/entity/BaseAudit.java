package com.followMe.common.entity;

import com.querydsl.core.annotations.QuerySupertype;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

/**
 * 생성/수정 시각 + 작성자 정보까지 관리하는 기반 엔티티.
 *
 * <p>소비자 서비스에서 {@code AuditorAware<String>} 빈을 등록해야 합니다.
 *
 * <pre>{@code
 * @Bean
 * public AuditorAware<String> auditorAware() {
 *     return () -> Optional.ofNullable(SecurityContextHolder.getContext())
 *         .map(ctx -> ctx.getAuthentication().getName());
 * }
 * }</pre>
 */
@Getter
@QuerySupertype
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAudit extends BaseTime {

    @CreatedBy
    @Column(nullable = false, updatable = false, length = 100)
    private UUID createdBy;

    @LastModifiedBy
    @Column(nullable = false, length = 100)
    private UUID updatedBy;

    @Column(length = 100)
    private UUID deletedBy;

    public void softDelete(UUID deletedBy) {
        super.softDelete();
        this.deletedBy = deletedBy;
    }
}