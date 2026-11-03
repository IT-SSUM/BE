package com.itssum.domain.notification.event;

import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.entity.PushRegistration;
import com.itssum.domain.notification.repository.NotificationSettingRepository;
import com.itssum.domain.notification.repository.PushRegistrationRepository;
import com.itssum.domain.notification.service.InvalidPushRegistrationCleaner;
import com.itssum.global.infra.fcm.PushDeliveryResult;
import com.itssum.global.infra.fcm.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationPublisher {

    private final NotificationSettingRepository settingRepository;
    private final PushRegistrationRepository registrationRepository;
    private final InvalidPushRegistrationCleaner registrationCleaner;
    private final ObjectProvider<PushSender> pushSender;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(NotificationCreatedEvent event) {
        PushSender sender = pushSender.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            boolean enabled = settingRepository.findById(event.receiverId())
                    .map(setting -> event.notification().type() == NotificationType.CHAT
                            ? setting.isChatEnabled() : setting.isMatchEnabled())
                    .orElse(true);
            if (!enabled) {
                return;
            }
            for (PushRegistration registration : registrationRepository.findAllByUser_Id(event.receiverId())) {
                try {
                    PushDeliveryResult result = sender.send(registration.getFid(), event.notification());
                    if (result == PushDeliveryResult.INVALID_REGISTRATION) {
                        registrationCleaner.deleteIfStillOwned(registration.getId(), registration.getFid(),
                                event.receiverId());
                    }
                } catch (RuntimeException exception) {
                    log.warn("기기 푸시 처리에 실패했습니다. notificationId={}, pushRegistrationId={}",
                            event.notification().notificationId(), registration.getId(), exception);
                }
            }
        } catch (RuntimeException exception) {
            log.warn("기기 푸시 발송 준비에 실패했습니다. notificationId={}",
                    event.notification().notificationId(), exception);
        }
    }
}
