package com.itssum.domain.notification.service;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiptReminderService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private final MatchRepository matchRepository;
    private final NotificationService notificationService;

    @Transactional
    public void sendDueReminders() {
        LocalDateTime now = LocalDateTime.now(SERVICE_ZONE);
        for (Match match : matchRepository.findDueReceiptReminders(
                MatchStatus.RESERVED, now, PageRequest.of(0, 100))) {
            match.markReceiptReminderSent(now);
            notificationService.createForMatch(match, match.getDonor().getUser(),
                    NotificationType.MATCH_RECEIVE_REQUIRED);
        }
    }
}
