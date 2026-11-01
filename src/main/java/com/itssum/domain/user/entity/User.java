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

    /** 로그인·토큰 재발급이 가능한 회원인지. 탈퇴 상태와 deleted_at 을 둘 다 본다. */
    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 회원 탈퇴. 같은 이메일로 다시 가입할 수 있도록 이메일을 회원마다 다른 자리표시 값으로 바꾸고(.invalid 는 실제로 쓰일 수 없는 도메인),
     * 비밀번호는 BCrypt 형식이 아닌 값으로 바꿔 어떤 비밀번호와도 맞지 않게 한다.
     */
    public void withdraw() {
        this.email = "withdrawn-" + id + "@itssum.invalid";
        this.password = "WITHDRAWN";
        this.status = UserStatus.WITHDRAWN;
        this.deletedAt = LocalDateTime.now();
    }
}
