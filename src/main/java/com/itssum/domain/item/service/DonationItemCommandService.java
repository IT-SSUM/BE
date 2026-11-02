package com.itssum.domain.item.service;

import com.itssum.domain.item.dto.request.CreateDonationItemRequest;
import com.itssum.domain.item.dto.request.UpdateDonationItemRequest;
import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.service.AddressGeocoder;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.kakao.GeoPoint;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 기부 물품 등록·수정·삭제. 카카오 좌표 변환과 S3 업로드 확인은 트랜잭션 밖에서 하고,
 * 저장만 트랜잭션으로 묶는다. 저장할 때 기부자 행을 잠가 같은 회원의 탈퇴와 겹치지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class DonationItemCommandService {

    private final MemberReader memberReader;
    private final ItemReferences itemReferences;
    private final ItemImages itemImages;
    private final AddressGeocoder addressGeocoder;
    private final DonationItemRepository donationItemRepository;
    private final MatchRepository matchRepository;
    private final TransactionTemplate transactionTemplate;

    public DonationItemDetailResponse create(Long donorId, CreateDonationItemRequest request) {
        memberReader.activeDonor(donorId);
        ItemCategory category = itemReferences.requireCategory(request.categoryId());
        Region region = itemReferences.requirePilotRegion(request.regionId());
        String unit = itemReferences.requireUnit(request.unit());
        List<String> imageKeys = distinct(request.imageKeys());
        itemImages.verifyUploaded(ItemImageKind.DONATION, donorId, imageKeys);
        String pickupAddress = request.pickupAddress().strip();
        GeoPoint location = addressGeocoder.locate(pickupAddress).orElse(null);

        return transactionTemplate.execute(status -> {
            Donor donor = memberReader.lockActiveDonor(donorId);
            DonationItem item = DonationItem.builder()
                    .donor(donor)
                    .category(category)
                    .region(region)
                    .pickupAddress(pickupAddress)
                    .pickupAddressDetail(blankToNull(request.pickupAddressDetail()))
                    .latitude(location == null ? null : location.latitude())
                    .longitude(location == null ? null : location.longitude())
                    .itemName(request.itemName().strip())
                    .quantity(request.quantity())
                    .unit(unit)
                    .conditionGrade(request.conditionGrade())
                    .description(blankToNull(request.description()))
                    .build();
            item.replaceImages(imageKeys);
            donationItemRepository.save(item);
            return DonationItemDetailResponse.of(item, true, itemImages::readUrl);
        });
    }

    /**
     * 수정 화면의 항목(설명, 물품 위치, 상세 주소, 사진)만 바꾼다. 보내지 않은 필드(null)는 그대로 둔다.
     * 새로 붙인 사진만 S3 업로드를 확인하고, 물품 위치가 바뀌었거나 좌표가 비어 있을 때만 좌표를 다시 구한다.
     */
    public DonationItemDetailResponse update(Long donorId, Long donationItemId, UpdateDonationItemRequest request) {
        memberReader.activeDonor(donorId);
        Snapshot current = transactionTemplate.execute(status -> {
            DonationItem item = ownedItem(donationItemRepository.findById(donationItemId).orElse(null), donorId);
            return new Snapshot(item.getPickupAddress(), item.getLatitude() != null, Set.copyOf(item.imageKeys()));
        });
        List<String> imageKeys = request.imageKeys() == null ? null : distinct(request.imageKeys());
        if (imageKeys != null) {
            List<String> added = imageKeys.stream().filter(key -> !current.imageKeys().contains(key)).toList();
            itemImages.verifyUploaded(ItemImageKind.DONATION, donorId, added);
        }
        String pickupAddress = request.pickupAddress() == null ? null : request.pickupAddress().strip();
        boolean relocated = pickupAddress != null
                && (!pickupAddress.equals(current.pickupAddress()) || !current.hasCoordinates());
        GeoPoint location = relocated ? addressGeocoder.locate(pickupAddress).orElse(null) : null;

        return transactionTemplate.execute(status -> {
            memberReader.lockActiveDonor(donorId);
            DonationItem item = lockOwnedItem(donationItemId, donorId);
            if (request.description() != null) {
                item.changeDescription(blankToNull(request.description()));
            }
            if (relocated) {
                item.relocate(pickupAddress,
                        location == null ? null : location.latitude(),
                        location == null ? null : location.longitude());
            }
            if (request.pickupAddressDetail() != null) {
                item.changePickupAddressDetail(blankToNull(request.pickupAddressDetail()));
            }
            if (imageKeys != null) {
                item.replaceImages(imageKeys);
            }
            return DonationItemDetailResponse.of(item, true, itemImages::readUrl);
        });
    }

    /**
     * soft delete. 끝나지 않은 매칭(응답 대기·거래중·예약중)이 걸려 있으면 막는다 (회원 탈퇴와 같은 기준).
     * 물품 행을 잠그고 확인하므로, 이미 있는 매칭의 수락·예약과 겹치지 않는다.
     */
    public void delete(Long donorId, Long donationItemId) {
        memberReader.activeDonor(donorId);
        transactionTemplate.executeWithoutResult(status -> {
            DonationItem item = lockOwnedItem(donationItemId, donorId);
            if (matchRepository.existsByDonationItemIdAndStatusNotIn(donationItemId, MatchStatus.FINISHED)) {
                throw new BusinessException(ErrorCode.ITEM_DELETE_BLOCKED);
            }
            item.delete();
        });
    }

    private DonationItem lockOwnedItem(Long donationItemId, Long donorId) {
        return ownedItem(donationItemRepository.findByIdForUpdate(donationItemId).orElse(null), donorId);
    }

    /** 없거나 삭제된 물품은 404, 남의 물품은 403. */
    private static DonationItem ownedItem(DonationItem item, Long donorId) {
        if (item == null || item.isDeleted()) {
            throw new BusinessException(ErrorCode.ITEM_NOT_FOUND);
        }
        if (!item.isOwnedBy(donorId)) {
            throw new BusinessException(ErrorCode.ITEM_NOT_OWNER);
        }
        return item;
    }

    /** 트랜잭션 밖에서 S3·카카오 호출 여부를 정하려고 읽어 두는 현재 값. */
    private record Snapshot(String pickupAddress, boolean hasCoordinates, Set<String> imageKeys) {
    }

    /** 같은 사진 key 가 두 번 오면 대표 사진·순서가 모호해지므로 400 으로 막는다. */
    private static List<String> distinct(List<String> imageKeys) {
        List<String> keys = imageKeys.stream().map(String::strip).toList();
        if (new HashSet<>(keys).size() != keys.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "같은 사진을 두 번 올릴 수 없습니다.");
        }
        return keys;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
