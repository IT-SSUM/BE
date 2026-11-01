package com.itssum.domain.notification.repository;

import com.itssum.domain.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    /** 회원 탈퇴 시 그 회원의 기기 토큰을 모두 지운다. 지운 행 수를 돌려준다. */
    @Modifying
    @Query("delete from PushToken p where p.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
