package com.itssum.domain.match.service;

import com.itssum.domain.chat.entity.ChatRoom;
import com.itssum.domain.chat.repository.ChatRoomRepository;
import com.itssum.domain.chat.service.ChatRoomService;
import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.NeededItemStatus;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.match.dto.request.ConfirmAppointmentRequest;
import com.itssum.domain.match.dto.request.CreateMatchRequest;
import com.itssum.domain.match.dto.request.UpdateMatchQuantityRequest;
import com.itssum.domain.match.dto.response.MatchAcceptedResponse;
import com.itssum.domain.match.dto.response.MatchAppointmentResponse;
import com.itssum.domain.match.dto.response.MatchCanceledResponse;
import com.itssum.domain.match.dto.response.MatchCompletedResponse;
import com.itssum.domain.match.dto.response.MatchCreatedResponse;
import com.itssum.domain.match.dto.response.MatchListResponse;
import com.itssum.domain.match.dto.response.MatchQuantityResponse;
import com.itssum.domain.match.dto.response.MatchRejectedResponse;
import com.itssum.domain.match.dto.response.MatchReservedResponse;
import com.itssum.domain.match.dto.response.MatchResponse;
import com.itssum.domain.match.dto.response.MatchWithdrawnResponse;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.service.NotificationService;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final DonationItemRepository donationItemRepository;
    private final NeededItemRepository neededItemRepository;
    private final ChatRoomService chatRoomService;
    private final ChatRoomRepository chatRoomRepository;
    private final NotificationService notificationService;

    @Transactional
    public MatchCreatedResponse create(User actor, CreateMatchRequest request) {
        // 물품 삭제와 겹치지 않게 두 물품 행을 잠그고 읽는다 (예약·완료와 같은 순서: 기부 물품 → 필요 물품)
        DonationItem donation = donationItemRepository.findByIdForUpdate(request.donationItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_INVALID_ITEM));
        NeededItem needed = neededItemRepository.findByIdForUpdate(request.neededItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_INVALID_ITEM));
        if (donation.getDeletedAt() != null || needed.getDeletedAt() != null
                || needed.getStatus() == NeededItemStatus.CLOSED
                || !donation.getCategory().getId().equals(needed.getCategory().getId())
                || !donation.getUnit().equals(needed.getUnit())
                || !meetsCondition(donation.getConditionGrade(), needed.getDesiredCondition())) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_ITEM);
        }
        RequesterType requester;
        if (actor.getRole() == UserRole.DONOR && actor.getId().equals(donation.getDonor().getId())) {
            requester = RequesterType.DONOR;
        } else if (actor.getRole() == UserRole.ORGANIZATION
                && actor.getId().equals(needed.getOrganization().getId())) {
            requester = RequesterType.ORGANIZATION;
        } else {
            throw new BusinessException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
        if (request.quantity() > donation.availableQuantity() || request.quantity() > needed.availableQuantity()) {
            throw new BusinessException(ErrorCode.MATCH_INSUFFICIENT_QUANTITY);
        }
        Match match = matchRepository.save(Match.builder()
                .donationItem(donation).neededItem(needed).donor(donation.getDonor())
                .organization(needed.getOrganization()).requestedBy(requester)
                .quantity(request.quantity()).message(request.message()).build());
        notificationService.createForMatch(match, actor, NotificationType.MATCH_REQUESTED);
        return MatchCreatedResponse.from(match);
    }

    @Transactional
    public MatchAcceptedResponse accept(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        requireRecipient(match, actor);
        requireStatus(match, MatchStatus.PENDING);
        if (match.getDonationItem().getDeletedAt() != null
                || match.getNeededItem().getDeletedAt() != null
                || match.getNeededItem().getStatus() == NeededItemStatus.CLOSED) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_ITEM);
        }
        match.accept();
        Long chatRoomId = chatRoomService.createForMatch(match).getId();
        notificationService.createForMatch(match, actor, NotificationType.MATCH_ACCEPTED);
        return MatchAcceptedResponse.from(match, chatRoomId);
    }

    @Transactional
    public MatchRejectedResponse reject(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        requireRecipient(match, actor);
        requireStatus(match, MatchStatus.PENDING);
        match.reject();
        notificationService.createForMatch(match, actor, NotificationType.MATCH_REJECTED);
        return MatchRejectedResponse.from(match);
    }

    @Transactional
    public MatchWithdrawnResponse withdraw(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        Long requesterId = match.getRequestedBy() == RequesterType.DONOR
                ? match.getDonor().getId() : match.getOrganization().getId();
        if (!requesterId.equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_NOT_REQUESTER);
        }
        requireStatus(match, MatchStatus.PENDING);
        match.cancel(actor, null);
        return MatchWithdrawnResponse.from(match);
    }

    @Transactional(readOnly = true)
    public List<MatchListResponse> list(User actor) {
        return matchRepository.findAllForUser(actor.getId()).stream().map(MatchListResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse detail(Long matchId, User actor) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        requireParticipant(match, actor);
        return responseWithRoom(match);
    }

    @Transactional
    public MatchReservedResponse reserve(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        if (!match.getDonor().getId().equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
        requireStatus(match, MatchStatus.TRADING);
        DonationItem donation = lockDonation(match);
        NeededItem needed = lockNeeded(match);
        // 모집이 종료된(CLOSED) 필요 물품이어도 이미 수락된 거래는 예약할 수 있다 (#44). 받은 수량을 다 채워 종료된
        // 물품은 신청 가능 수량이 남지 않아 아래 수량 검사에서 막힌다.
        if (donation.getDeletedAt() != null || needed.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_ITEM);
        }
        if (match.getQuantity() > donation.availableQuantity() || match.getQuantity() > needed.availableQuantity()) {
            throw new BusinessException(ErrorCode.MATCH_INSUFFICIENT_QUANTITY);
        }
        donation.reserve(match.getQuantity());
        needed.reserve(match.getQuantity());
        match.reserve();
        notificationService.createForMatch(match, actor, NotificationType.MATCH_RESERVED);
        return MatchReservedResponse.from(match);
    }

    @Transactional
    public MatchQuantityResponse updateQuantity(Long matchId, User actor, UpdateMatchQuantityRequest request) {
        Match match = findForUpdate(matchId, actor);
        if (!match.getDonor().getId().equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_QUANTITY_NOT_DONOR);
        }
        requireStatus(match, MatchStatus.TRADING);
        DonationItem donation = lockDonation(match);
        NeededItem needed = lockNeeded(match);
        // 모집이 종료된 필요 물품이어도 거래중 합의 수량은 바꿀 수 있다 (#44). 수량 검사는 그대로 한다.
        if (donation.getDeletedAt() != null || needed.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_ITEM);
        }
        if (request.quantity() > donation.availableQuantity() || request.quantity() > needed.availableQuantity()) {
            throw new BusinessException(ErrorCode.MATCH_INSUFFICIENT_QUANTITY);
        }
        match.changeQuantity(request.quantity());
        return MatchQuantityResponse.from(match);
    }

    @Transactional
    public MatchCompletedResponse complete(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        if (!match.getOrganization().getId().equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
        requireStatus(match, MatchStatus.RESERVED);
        lockDonation(match).complete(match.getQuantity());
        lockNeeded(match).complete(match.getQuantity());
        match.complete();
        notificationService.createForMatch(match, actor, NotificationType.MATCH_COMPLETED);
        return MatchCompletedResponse.from(match);
    }

    @Transactional
    public MatchCanceledResponse cancel(Long matchId, User actor, String reason) {
        Match match = findForUpdate(matchId, actor);
        if (match.getStatus() != MatchStatus.TRADING && match.getStatus() != MatchStatus.RESERVED) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
        }
        if (match.getStatus() == MatchStatus.RESERVED) {
            lockDonation(match).release(match.getQuantity());
            lockNeeded(match).release(match.getQuantity());
        }
        match.cancel(actor, reason);
        notificationService.createForMatch(match, actor, NotificationType.MATCH_CANCELED);
        return MatchCanceledResponse.from(match);
    }

    @Transactional
    public MatchAppointmentResponse confirmAppointment(Long matchId, User actor, ConfirmAppointmentRequest request) {
        Match match = findForUpdate(matchId, actor);
        requireStatus(match, MatchStatus.RESERVED);
        LocalDateTime scheduledAt = LocalDateTime.ofInstant(request.scheduledAt().toInstant(),
                ZoneId.of("Asia/Seoul"));
        match.confirmAppointment(request.deliveryMethod(), scheduledAt,
                request.deliveryPlace().trim(), request.receiverName());
        chatRoomService.createAppointmentCard(match, actor);
        notificationService.createForMatch(match, actor, NotificationType.MATCH_SCHEDULED);
        return MatchAppointmentResponse.from(match);
    }

    private MatchResponse responseWithRoom(Match match) {
        Long roomId = chatRoomRepository.findByMatch_Id(match.getId())
                .map(ChatRoom::getId).orElse(null);
        return MatchResponse.from(match, roomId);
    }

    private Match findForUpdate(Long matchId, User actor) {
        Match match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        requireParticipant(match, actor);
        return match;
    }

    private void requireParticipant(Match match, User actor) {
        if (!match.getDonor().getId().equals(actor.getId())
                && !match.getOrganization().getId().equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
    }

    private void requireRecipient(Match match, User actor) {
        Long recipientId = match.getRequestedBy() == RequesterType.DONOR
                ? match.getOrganization().getId() : match.getDonor().getId();
        if (!recipientId.equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_NOT_RECIPIENT);
        }
    }

    private void requireStatus(Match match, MatchStatus status) {
        if (match.getStatus() != status) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
        }
    }

    private DonationItem lockDonation(Match match) {
        return donationItemRepository.findByIdForUpdate(match.getDonationItem().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_INVALID_ITEM));
    }

    private NeededItem lockNeeded(Match match) {
        return neededItemRepository.findByIdForUpdate(match.getNeededItem().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_INVALID_ITEM));
    }

    private boolean meetsCondition(ConditionGrade actual, ConditionGrade minimum) {
        return actual.ordinal() <= minimum.ordinal();
    }
}
