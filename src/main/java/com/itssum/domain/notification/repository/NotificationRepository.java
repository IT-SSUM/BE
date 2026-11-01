package com.itssum.domain.notification.repository;

import com.itssum.domain.notification.entity.Notification;
import com.itssum.domain.notification.entity.NotificationTargetType;
import com.itssum.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiver_IdOrderByIdDesc(Long receiverId, Pageable pageable);

    List<Notification> findByReceiver_IdAndIdLessThanOrderByIdDesc(Long receiverId, Long beforeId, Pageable pageable);

    long countByReceiver_IdAndIsReadFalse(Long receiverId);

    List<Notification> findAllByReceiver_IdAndTypeAndTargetTypeAndTargetId(
            Long receiverId, NotificationType type, NotificationTargetType targetType, Long targetId);

    @Modifying
    @Query("update Notification n set n.isRead = true, n.readAt = :readAt "
            + "where n.receiver.id = :receiverId and n.type = :type and n.targetType = :targetType "
            + "and n.targetId = :roomId and n.chatMessageId <= :messageId and n.isRead = false")
    int markChatReadThrough(@Param("receiverId") Long receiverId, @Param("roomId") Long roomId,
                            @Param("messageId") Long messageId, @Param("readAt") LocalDateTime readAt,
                            @Param("type") NotificationType type,
                            @Param("targetType") NotificationTargetType targetType);

    @Modifying
    @Query("update Notification n set n.isRead = true, n.readAt = :readAt "
            + "where n.receiver.id = :receiverId and n.isRead = false")
    int markAllRead(@Param("receiverId") Long receiverId, @Param("readAt") LocalDateTime readAt);
}
