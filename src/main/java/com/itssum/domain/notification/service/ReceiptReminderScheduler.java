package com.itssum.domain.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReceiptReminderScheduler {

    private final ReceiptReminderService receiptReminderService;

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void sendDueReminders() {
        receiptReminderService.sendDueReminders();
    }
}
