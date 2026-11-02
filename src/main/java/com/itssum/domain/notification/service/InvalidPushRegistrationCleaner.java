package com.itssum.domain.notification.service;

import com.itssum.domain.notification.repository.PushRegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvalidPushRegistrationCleaner {

    private final PushRegistrationRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteIfStillOwned(Long id, String fid, Long userId) {
        repository.deleteIfStillOwned(id, fid, userId);
    }
}
