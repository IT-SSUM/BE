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
import com.itssum.domain.match.dto.response.MatchAcceptedResponse;
import com.itssum.domain.match.dto.response.MatchCreatedResponse;
import com.itssum.domain.match.dto.response.MatchRejectedResponse;
import com.itssum.domain.match.dto.response.MatchResponse;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    @Transactional
    public MatchCreatedResponse create(User actor, CreateMatchRequest request) {
        DonationItem donation = donationItemRepository.findById(request.donationItemId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_INVALID_ITEM));
        NeededItem needed = neededItemRepository.findById(request.neededItemId())
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
        return MatchAcceptedResponse.from(match, chatRoomId);
    }

    @Transactional
    public MatchRejectedResponse reject(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        requireRecipient(match, actor);
        requireStatus(match, MatchStatus.PENDING);
        match.reject();
        return MatchRejectedResponse.from(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> list(User actor) {
        List<Match> matches = matchRepository.findAllForUser(actor.getId());
        if (matches.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> roomIds = chatRoomRepository.findByMatch_IdIn(
                matches.stream().map(Match::getId).toList()).stream()
                .collect(Collectors.toMap(room -> room.getMatch().getId(), ChatRoom::getId));
        return matches.stream().map(match -> MatchResponse.from(match, roomIds.get(match.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public MatchResponse detail(Long matchId, User actor) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
        requireParticipant(match, actor);
        return responseWithRoom(match);
    }

    @Transactional
    public MatchResponse reserve(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        if (!match.getDonor().getId().equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
        requireStatus(match, MatchStatus.TRADING);
        DonationItem donation = lockDonation(match);
        NeededItem needed = lockNeeded(match);
        if (donation.getDeletedAt() != null || needed.getDeletedAt() != null
                || needed.getStatus() == NeededItemStatus.CLOSED) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_ITEM);
        }
        if (match.getQuantity() > donation.availableQuantity() || match.getQuantity() > needed.availableQuantity()) {
            throw new BusinessException(ErrorCode.MATCH_INSUFFICIENT_QUANTITY);
        }
        donation.reserve(match.getQuantity());
        needed.reserve(match.getQuantity());
        match.reserve();
        return responseWithRoom(match);
    }

    @Transactional
    public MatchResponse complete(Long matchId, User actor) {
        Match match = findForUpdate(matchId, actor);
        if (!match.getOrganization().getId().equals(actor.getId())) {
            throw new BusinessException(ErrorCode.MATCH_NOT_PARTICIPANT);
        }
        requireStatus(match, MatchStatus.RESERVED);
        lockDonation(match).complete(match.getQuantity());
        lockNeeded(match).complete(match.getQuantity());
        match.complete();
        return responseWithRoom(match);
    }

    @Transactional
    public MatchResponse cancel(Long matchId, User actor, String reason) {
        Match match = findForUpdate(matchId, actor);
        if (match.getStatus() != MatchStatus.TRADING && match.getStatus() != MatchStatus.RESERVED) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_STATUS);
        }
        if (match.getStatus() == MatchStatus.RESERVED) {
            lockDonation(match).release(match.getQuantity());
            lockNeeded(match).release(match.getQuantity());
        }
        match.cancel(actor, reason);
        return responseWithRoom(match);
    }

    @Transactional
    public MatchResponse confirmAppointment(Long matchId, User actor, ConfirmAppointmentRequest request) {
        Match match = findForUpdate(matchId, actor);
        requireStatus(match, MatchStatus.RESERVED);
        LocalDateTime scheduledAt = LocalDateTime.ofInstant(request.scheduledAt().toInstant(),
                ZoneId.of("Asia/Seoul"));
        match.confirmAppointment(request.deliveryMethod(), scheduledAt,
                request.deliveryPlace().trim(), request.receiverName());
        chatRoomService.createAppointmentCard(match, actor);
        return responseWithRoom(match);
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
