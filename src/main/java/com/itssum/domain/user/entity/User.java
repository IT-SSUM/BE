package com.itssum.domain.user.entity;

import com.itssum.global.common.BaseTimeEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "users",
        comment = "회원",
        uniqueConstraints = @UniqueConstraint(name = "UQ_users_email", columnNames = "email")
)
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "가입 일시"))
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", comment = "회원 ID")
    private Long id;

    @Column(nullable = false, length = 255, comment = "로그인 이메일")
    private String email;

    @Column(nullable = false, length = 255, comment = "BCrypt 해시")
    private String password;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, comment = "DONOR | ORGANIZATION (가입 유형 선택)")
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'ACTIVE'")
    @Column(nullable = false, length = 20, comment = "ACTIVE | WITHDRAWN")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(secondPrecision = 0, comment = "탈퇴 일시 (soft delete)")
    private LocalDateTime deletedAt;

    @Builder
    private User(String email, String password, UserRole role) {
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
