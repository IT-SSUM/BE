package com.itssum.domain.notification.repository;

import com.itssum.domain.notification.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
}
