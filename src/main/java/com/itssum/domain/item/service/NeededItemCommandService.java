package com.itssum.domain.item.service;

import com.itssum.domain.item.dto.request.CreateNeededItemRequest;
import com.itssum.domain.item.dto.request.UpdateNeededItemRequest;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.NeededItemStatus;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.notification.entity.NotificationType;
import com.itssum.domain.notification.service.NotificationService;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 필요 물품 등록·수정·삭제·모집 종료. 거리는 기관 주소 좌표로 계산하므로 필요 물품은 좌표를 구하지 않는다.
 * S3 업로드 확인은 트랜잭션 밖에서 하고, 저장할 때 기관 행을 잠가 같은 회원의 탈퇴와 겹치지 않게 한다.
 * 모집 종료는 기관 → 필요 물품 → 응답 대기 매칭 순서로 잠근다. 매칭 생성은 기부 물품 → 필요 물품 순서로 잠그므로,
 * 종료를 기다린 생성 요청은 종료가 커밋된 뒤 모집 종료 상태를 보고 막힌다.
 */
@Service
@RequiredArgsConstructor
public class NeededItemCommandService {

    private final MemberReader memberReader;
    private final ItemReferences itemReferences;
    private final ItemImages itemImages;
    private final NeededItemRepository neededItemRepository;
    private final MatchRepository matchRepository;
    private final TransactionTemplate transactionTemplate;
    private final NotificationService notificationService;

    public NeededItemDetailResponse create(Long organizationId, CreateNeededItemRequest request) {
        memberReader.activeOrganization(organizationId);
        ItemCategory category = itemReferences.requireCategory(request.categoryId());
        Region region = itemReferences.requirePilotRegion(request.regionId());
        String unit = itemReferences.requireUnit(request.unit());
        String referenceImageKey = ItemTexts.blankToNull(request.referenceImageKey());
        if (referenceImageKey != null) {
            itemImages.verifyUploaded(ItemImageKind.NEEDED, organizationId, List.of(referenceImageKey));
        }

        return transactionTemplate.execute(status -> {
            Organization organization = memberReader.lockActiveOrganization(organizationId);
            NeededItem item = NeededItem.builder()
                    .organization(organization)
                    .category(category)
                    .region(region)
                    .itemName(request.itemName().strip())
                    .requiredQuantity(request.requiredQuantity())
                    .unit(unit)
                    .desiredCondition(request.desiredCondition())
                    .description(ItemTexts.blankToNull(request.description()))
                    .referenceImageUrl(referenceImageKey)
                    .build();
            neededItemRepository.save(item);
            return NeededItemDetailResponse.of(item, true, itemImages::readUrl);
        });
    }

    /**
     * 수정 화면의 항목(필요한 이유, 수령 장소, 참고 사진)만 바꾼다. 보내지 않은 필드(null)는 그대로 두고, 빈 값이면 비운다.
     * 참고 사진은 지금과 다른 key 일 때만 S3 업로드를 확인한다.
     */
    public NeededItemDetailResponse update(Long organizationId, Long neededItemId, UpdateNeededItemRequest request) {
        memberReader.activeOrganization(organizationId);
        String currentImageKey = transactionTemplate.execute(status ->
                ownedItem(neededItemRepository.findById(neededItemId).orElse(null), organizationId).getReferenceImageUrl());
        String referenceImageKey = ItemTexts.blankToNull(request.referenceImageKey());
        if (referenceImageKey != null && !referenceImageKey.equals(currentImageKey)) {
            itemImages.verifyUploaded(ItemImageKind.NEEDED, organizationId, List.of(referenceImageKey));
        }

        return transactionTemplate.execute(status -> {
            memberReader.lockActiveOrganization(organizationId);
            NeededItem item = lockOwnedItem(neededItemId, organizationId);
            if (request.description() != null) {
                item.changeDescription(ItemTexts.blankToNull(request.description()));
            }
            if (request.receiveAddress() != null) {
                item.changeReceiveAddress(ItemTexts.blankToNull(request.receiveAddress()));
            }
            if (request.referenceImageKey() != null) {
                item.changeReferenceImage(referenceImageKey);
            }
            return NeededItemDetailResponse.of(item, true, itemImages::readUrl);
        });
    }

    /**
     * soft delete. 끝나지 않은 매칭(응답 대기·거래중·예약중)이 걸려 있으면 막는다 (회원 탈퇴와 같은 기준).
     * 물품 행을 잠그고 확인하므로, 이미 있는 매칭의 수락·예약과 겹치지 않는다.
     */
    public void delete(Long organizationId, Long neededItemId) {
        memberReader.activeOrganization(organizationId);
        transactionTemplate.executeWithoutResult(status -> {
            NeededItem item = lockOwnedItem(neededItemId, organizationId);
            if (matchRepository.existsByNeededItemIdAndStatusNotIn(neededItemId, MatchStatus.FINISHED)) {
                throw new BusinessException(ErrorCode.ITEM_DELETE_BLOCKED);
            }
            item.delete();
        });
    }

    /**
     * 기관이 모집을 직접 종료한다. 응답 대기 중인 기부자 제안은 거절하고 거절 알림을 보내며, 기관이 보낸 요청은 철회한다.
     * 거래중·예약중 거래는 그대로 둔다(종료 뒤에도 예약·수령까지 진행된다). 이미 종료된 물품은 아무것도 바꾸지 않는다.
     */
    public NeededItemDetailResponse close(Long organizationId, Long neededItemId) {
        memberReader.activeOrganization(organizationId);
        return transactionTemplate.execute(status -> {
            Organization organization = memberReader.lockActiveOrganization(organizationId);
            NeededItem item = lockOwnedItem(neededItemId, organizationId);
            if (item.getStatus() != NeededItemStatus.CLOSED) {
                item.close();
                User actor = organization.getUser();
                for (Match pending : matchRepository.findByNeededItemIdAndStatusForUpdate(neededItemId,
                        MatchStatus.PENDING)) {
                    if (pending.getRequestedBy() == RequesterType.DONOR) {
                        pending.reject();
                        notificationService.createForMatch(pending, actor, NotificationType.MATCH_REJECTED);
                    } else {
                        pending.cancel(actor, null);
                    }
                }
            }
            return NeededItemDetailResponse.of(item, true, itemImages::readUrl);
        });
    }

    private NeededItem lockOwnedItem(Long neededItemId, Long organizationId) {
        return ownedItem(neededItemRepository.findByIdForUpdate(neededItemId).orElse(null), organizationId);
    }

    /** 없거나 삭제된 물품은 404, 남의 물품은 403. */
    private static NeededItem ownedItem(NeededItem item, Long organizationId) {
        if (item == null || item.isDeleted()) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (!item.isOwnedBy(organizationId)) {
            throw new BusinessException(ErrorCode.ITEM_NOT_OWNER);
        }
        return item;
    }
}
