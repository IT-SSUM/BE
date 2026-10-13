package com.itssum.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * created_at 만 있는 테이블용. 컬럼 COMMENT가 테이블마다 다르면 엔티티에서 @AttributeOverride로 덮어쓴다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedTimeEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false, secondPrecision = 0, comment = "생성 일시")
    private LocalDateTime createdAt;
}
