package com.itssum.domain.notification.repository;

import com.itssum.domain.notification.entity.PushRegistration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushRegistrationRepository extends JpaRepository<PushRegistration, Long> {

    Optional<PushRegistration> findByFid(String fid);

    List<PushRegistration> findAllByUser_Id(Long userId);

    @Modifying
    @Query(value = "INSERT INTO push_registrations (user_id, fid, device_type, created_at, updated_at) "
            + "VALUES (:userId, :fid, :deviceType, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
            + "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), device_type = VALUES(device_type), "
            + "updated_at = CURRENT_TIMESTAMP", nativeQuery = true)
    int register(@Param("userId") Long userId, @Param("fid") String fid,
                 @Param("deviceType") String deviceType);

    @Modifying
    @Query("delete from PushRegistration p where p.id = :id and p.fid = :fid and p.user.id = :userId")
    int deleteIfStillOwned(@Param("id") Long id, @Param("fid") String fid, @Param("userId") Long userId);

    /** 회원 탈퇴 시 그 회원의 기기 등록을 모두 지운다. 지운 행 수를 돌려준다. */
    @Modifying
    @Query("delete from PushRegistration p where p.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
