package com.itssum.domain.notification.repository;

import com.itssum.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    @Modifying
    @Query(value = "INSERT IGNORE INTO notification_settings "
            + "(user_id, match_enabled, chat_enabled, created_at, updated_at) "
            + "VALUES (:userId, TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", nativeQuery = true)
    int ensureDefault(@Param("userId") Long userId);
}
