package com.itssum.domain.auth.repository;

import com.itssum.domain.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("select r from RefreshToken r join fetch r.user where r.tokenHash = :tokenHash")
    Optional<RefreshToken> findWithUserByTokenHash(@Param("tokenHash") String tokenHash);

    /** 지운 행 수. 0 이면 다른 요청이 먼저 교체한 것이다. */
    @Modifying
    @Query("delete from RefreshToken r where r.id = :id")
    int deleteByIdAndReturnCount(@Param("id") Long id);

    @Modifying
    @Query("delete from RefreshToken r where r.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    /** 회원 탈퇴 시 모든 기기에서 로그아웃시킨다. */
    @Modifying
    @Query("delete from RefreshToken r where r.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from RefreshToken r where r.user.id = :userId and r.expiresAt < :now")
    int deleteExpiredByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
