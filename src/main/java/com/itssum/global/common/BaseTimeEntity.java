package com.itssum.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * created_at + updated_at 이 있는 테이블용.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedTimeEntity {

    @LastModifiedDate
    @Column(nullable = false, secondPrecision = 0, comment = "수정 일시")
    private LocalDateTime updatedAt;
}
