package com.itssum.domain.notification.service;

import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.notification.dto.response.NotificationPageResponse;
import com.itssum.domain.notification.dto.response.NotificationResponse;
import com.itssum.domain.notification.dto.response.UnreadNotificationCountResponse;
import com.itssum.domain.notification.entity.Notification;
import com.itssum.domain.notification.entity.NotificationTargetType;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.event.NotificationCreatedEvent;
import com.itssum.domain.notification.repository.NotificationRepository;
import com.itssum.domain.user.entity.User;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private static final DateTimeFormatter SCHEDULE_FORMAT =
            DateTimeFormatter.ofPattern("M월 d일 EEEE a h시", Locale.KOREAN);

    @Transactional(propagation = Propagation.MANDATORY)
    public void createForMatch(Match match, User actor, NotificationType type) {
        User donor = match.getDonor().getUser();
        User organization = match.getOrganization().getUser();
        User receiver;
        if (donor.getId().equals(actor.getId())) {
            receiver = organization;
        } else if (organization.getId().equals(actor.getId())) {
            receiver = donor;
        } else {
            throw new BusinessException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
        String item = match.getDonationItem().getItemName() + " " + match.getQuantity()
                + match.getDonationItem().getUnit();
        String otherName = receiver.getId().equals(donor.getId())
                ? match.getOrganization().getName() : match.getDonor().getName() + " 님";
        String title = titleForMatch(match, type);
        String content = contentForMatch(match, type, item, otherName);
        Notification saved = notificationRepository.save(Notification.builder()
                .receiver(receiver).type(type).title(title).content(content)
                .targetType(NotificationTargetType.MATCH).targetId(match.getId()).build());
        eventPublisher.publishEvent(new NotificationCreatedEvent(receiver.getId(), NotificationResponse.from(saved)));
    }

    private String titleForMatch(Match match, NotificationType type) {
        return switch (type) {
            case MATCH_REQUESTED -> match.getRequestedBy() == RequesterType.DONOR
                    ? "새로운 기부 제안" : "새로운 기부 요청";
            case MATCH_ACCEPTED -> match.getRequestedBy() == RequesterType.DONOR
                    ? "기부 제안을 수락했어요" : "기부 요청을 수락했어요";
            case MATCH_SCHEDULED -> "전달 일정이 정해졌어요";
            case MATCH_RESERVED -> "거래가 예약됐어요";
            case MATCH_RECEIVE_REQUIRED -> "수령 여부를 확인해 주세요";
            case MATCH_COMPLETED -> "전달 완료 · 수령 확인";
            case MATCH_CANCELED -> match.getReservedAt() == null
                    ? "거래 취소" : "거래 취소 · 예약 수량 반환";
            default -> type.getDescription();
        };
    }

    private String contentForMatch(Match match, NotificationType type, String item, String otherName) {
        return switch (type) {
            case MATCH_REQUESTED -> match.getRequestedBy() == RequesterType.DONOR
                    ? otherName + particle(otherName, "이", "가") + " "
                    + item + particle(item, "을", "를") + " 제안했어요."
                    : otherName + "에서 " + item + " 기부를 요청했어요.";
            case MATCH_ACCEPTED -> otherName + particle(otherName, "이", "가") + " " + item + " 기부 "
                    + (match.getRequestedBy() == RequesterType.DONOR ? "제안" : "요청")
                    + "을 수락했어요. 거래중이며 수량은 아직 확보되지 않았어요.";
            case MATCH_SCHEDULED -> match.getScheduledAt() == null ? item
                    : item + " · " + match.getScheduledAt().format(
                    SCHEDULE_FORMAT)
                    + (match.getDeliveryMethod() == DeliveryMethod.DIRECT ? " 직접 전달" : " 택배 전달");
            case MATCH_RESERVED -> item + particle(item, "이", "가")
                    + " 예약됐어요. 전달 일정을 협의해 주세요.";
            case MATCH_RECEIVE_REQUIRED -> item + particle(item, "을", "를")
                    + " 받았다면 확인해 주세요.";
            case MATCH_COMPLETED -> item + particle(item, "이", "가") + " "
                    + match.getOrganization().getName() + "에 전달 완료됐어요.";
            case MATCH_CANCELED -> match.getReservedAt() == null
                    ? otherName + particle(otherName, "과", "와") + "의 거래가 취소됐어요."
                    : otherName + particle(otherName, "과", "와") + "의 예약중 거래가 취소되어 "
                    + match.getQuantity() + match.getDonationItem().getUnit()
                    + particle(match.getDonationItem().getUnit(), "이", "가")
                    + " 신청 가능 수량으로 반환됐어요.";
            default -> item;
        };
    }

    private String particle(String text, String withFinalConsonant, String withoutFinalConsonant) {
        int last = text.codePointBefore(text.length());
        return last >= '가' && last <= '힣' && (last - '가') % 28 != 0
                ? withFinalConsonant : withoutFinalConsonant;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse list(Long userId, Long beforeId, int size) {
        if (size < 1 || size > 100 || (beforeId != null && beforeId < 1)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        PageRequest page = PageRequest.of(0, size + 1);
        List<Notification> found = beforeId == null
                ? notificationRepository.findByReceiver_IdOrderByIdDesc(userId, page)
                : notificationRepository.findByReceiver_IdAndIdLessThanOrderByIdDesc(userId, beforeId, page);
        boolean hasNext = found.size() > size;
        List<NotificationResponse> notifications = found.subList(0, Math.min(found.size(), size))
                .stream().map(NotificationResponse::from).toList();
        Long nextCursor = hasNext ? notifications.getLast().notificationId() : null;
        return new NotificationPageResponse(notifications, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse unreadCount(Long userId) {
        return new UnreadNotificationCountResponse(notificationRepository.countByReceiver_IdAndIsReadFalse(userId));
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notification.markRead();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId, LocalDateTime.now());
    }
}
