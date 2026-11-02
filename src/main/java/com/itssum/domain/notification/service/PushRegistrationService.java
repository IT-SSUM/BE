package com.itssum.domain.notification.service;

import com.itssum.domain.notification.dto.request.RegisterPushRegistrationRequest;
import com.itssum.domain.notification.dto.response.PushRegistrationResponse;
import com.itssum.domain.notification.entity.PushRegistration;
import com.itssum.domain.notification.repository.PushRegistrationRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushRegistrationService {

    private final PushRegistrationRepository repository;

    @Transactional
    public PushRegistrationResponse register(Long userId, RegisterPushRegistrationRequest request) {
        String fid = request.fid().strip();
        repository.register(userId, fid, request.deviceType().name());
        PushRegistration saved = repository.findByFid(fid)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        return PushRegistrationResponse.from(saved);
    }

    @Transactional
    public void delete(Long userId, Long pushRegistrationId) {
        PushRegistration registration = repository.findById(pushRegistrationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PUSH_REGISTRATION_NOT_FOUND));
        if (repository.deleteIfStillOwned(registration.getId(), registration.getFid(), userId) == 0) {
            throw new BusinessException(ErrorCode.PUSH_REGISTRATION_NOT_FOUND);
        }
    }
}
