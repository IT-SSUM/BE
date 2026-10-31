package com.itssum.domain.auth.repository;

import com.itssum.domain.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** 회원 탈퇴 시 그 회원의 비밀번호 재설정 토큰을 모두 지운다. */
    @Modifying
    @Query("delete from PasswordResetToken p where p.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
