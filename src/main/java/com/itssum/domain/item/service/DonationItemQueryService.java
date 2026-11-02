package com.itssum.domain.item.service;

import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.dto.response.DonationItemSummaryResponse;
import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.domain.item.dto.response.MyDonationItemResponse;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.DonationItemImage;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.DonationItemImageRepository;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기부 물품 탐색·상세·내 목록. 탐색과 상세는 로그인한 회원 누구나, 내 목록은 기부자 본인만 본다. */
@Service
@RequiredArgsConstructor
public class DonationItemQueryService {

    private static final int REPRESENTATIVE_SORT_ORDER = 0;

    private final MemberReader memberReader;
    private final DonationItemRepository donationItemRepository;
    private final DonationItemImageRepository donationItemImageRepository;
    private final ItemImages itemImages;

    @Transactional(readOnly = true)
    public ItemPageResponse<DonationItemSummaryResponse> browse(Long viewerId, ItemSearchCondition condition,
                                                                Long beforeId, int size) {
        Pageable pageable = ItemPaging.request(beforeId, size);
        memberReader.activeUser(viewerId);
        List<DonationItem> fetched = donationItemRepository.search(condition.likePattern(), condition.categoryId(),
                condition.regionId(), condition.condition(), beforeId, pageable);
        return ItemPaging.page(fetched, size, DonationItem::getId,
                items -> withRepresentativeImages(items, DonationItemSummaryResponse::of));
    }

    /** 삭제된 물품만 404. 예약마감·전달 완료 물품도 마이페이지·채팅에서 들어오므로 상세는 연다. */
    @Transactional(readOnly = true)
    public DonationItemDetailResponse detail(Long viewerId, Long donationItemId) {
        memberReader.activeUser(viewerId);
        DonationItem item = donationItemRepository.findById(donationItemId)
                .filter(found -> !found.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        return DonationItemDetailResponse.of(item, item.isOwnedBy(viewerId), itemImages::readUrl);
    }

    @Transactional(readOnly = true)
    public ItemPageResponse<MyDonationItemResponse> mine(Long donorId, Long categoryId, Long beforeId, int size) {
        Pageable pageable = ItemPaging.request(beforeId, size);
        memberReader.activeDonor(donorId);
        List<DonationItem> fetched = donationItemRepository.findMine(donorId, categoryId, beforeId, pageable);
        return ItemPaging.page(fetched, size, DonationItem::getId,
                items -> withRepresentativeImages(items, MyDonationItemResponse::of));
    }

    /** 목록 물품들의 대표 사진을 한 번에 읽어 카드로 만든다 (물품마다 사진을 읽지 않도록). */
    private <T> List<T> withRepresentativeImages(List<DonationItem> items, BiFunction<DonationItem, String, T> card) {
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, String> keys = donationItemImageRepository
                .findByDonationItemIdInAndSortOrder(items.stream().map(DonationItem::getId).toList(),
                        REPRESENTATIVE_SORT_ORDER)
                .stream()
                .collect(Collectors.toMap(image -> image.getDonationItem().getId(), DonationItemImage::getImageUrl,
                        (first, second) -> first));
        return items.stream()
                .map(item -> card.apply(item, itemImages.readUrl(keys.get(item.getId()))))
                .toList();
    }
}
