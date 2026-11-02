package com.itssum.domain.item.service;

import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.domain.item.dto.response.MyNeededItemResponse;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.dto.response.NeededItemSummaryResponse;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 필요 물품 탐색·상세·우리 기관 목록. 탐색과 상세는 로그인한 회원 누구나, 우리 기관 목록은 기관 본인만 본다. */
@Service
@RequiredArgsConstructor
public class NeededItemQueryService {

    private final MemberReader memberReader;
    private final NeededItemRepository neededItemRepository;
    private final ItemImages itemImages;

    @Transactional(readOnly = true)
    public ItemPageResponse<NeededItemSummaryResponse> browse(Long viewerId, ItemSearchCondition condition,
                                                              Long beforeId, int size) {
        Pageable pageable = ItemPaging.request(beforeId, size);
        memberReader.activeUser(viewerId);
        List<NeededItem> fetched = neededItemRepository.search(condition.likePattern(), condition.categoryId(),
                condition.regionId(), condition.condition(), beforeId, pageable);
        return ItemPaging.page(fetched, size, NeededItem::getId, items -> items.stream()
                .map(item -> NeededItemSummaryResponse.of(item, itemImages.readUrl(item.getReferenceImageUrl())))
                .toList());
    }

    /** 삭제된 물품만 404. 예약마감·모집 종료 물품도 마이페이지·채팅에서 들어오므로 상세는 연다. */
    @Transactional(readOnly = true)
    public NeededItemDetailResponse detail(Long viewerId, Long neededItemId) {
        memberReader.activeUser(viewerId);
        NeededItem item = neededItemRepository.findById(neededItemId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        return NeededItemDetailResponse.of(item, item.isOwnedBy(viewerId), itemImages::readUrl);
    }

    @Transactional(readOnly = true)
    public ItemPageResponse<MyNeededItemResponse> mine(Long organizationId, Long categoryId, Long beforeId, int size) {
        Pageable pageable = ItemPaging.request(beforeId, size);
        memberReader.activeOrganization(organizationId);
        List<NeededItem> fetched = neededItemRepository.findMine(organizationId, categoryId, beforeId, pageable);
        return ItemPaging.page(fetched, size, NeededItem::getId, items -> items.stream()
                .map(item -> MyNeededItemResponse.of(item, itemImages.readUrl(item.getReferenceImageUrl())))
                .toList());
    }
}
