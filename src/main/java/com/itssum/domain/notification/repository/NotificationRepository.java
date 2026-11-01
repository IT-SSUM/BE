package com.itssum.domain.notification.repository;

import com.itssum.domain.notification.entity.Notification;
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

    @Modifying
    @Query("update Notification n set n.isRead = true, n.readAt = :readAt "
            + "where n.receiver.id = :receiverId and n.isRead = false")
    int markAllRead(@Param("receiverId") Long receiverId, @Param("readAt") LocalDateTime readAt);
}
