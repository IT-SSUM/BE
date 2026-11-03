package com.itssum.domain.notification.service;

import com.itssum.domain.notification.dto.request.UpdateNotificationSettingRequest;
import com.itssum.domain.notification.dto.response.NotificationSettingResponse;
import com.itssum.domain.notification.entity.NotificationSetting;
import com.itssum.domain.notification.repository.NotificationSettingRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingService {

    private final NotificationSettingRepository repository;

    @Transactional
    public NotificationSettingResponse get(Long userId) {
        return NotificationSettingResponse.from(requireSetting(userId));
    }

    @Transactional
    public NotificationSettingResponse update(Long userId, UpdateNotificationSettingRequest request) {
        if (request == null || (request.matchEnabled() == null && request.chatEnabled() == null)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        NotificationSetting setting = requireSetting(userId);
        setting.update(request.matchEnabled() == null ? setting.isMatchEnabled() : request.matchEnabled(),
                request.chatEnabled() == null ? setting.isChatEnabled() : request.chatEnabled());
        return NotificationSettingResponse.from(setting);
    }

    private NotificationSetting requireSetting(Long userId) {
        repository.ensureDefault(userId);
        return repository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
