package com.itssum.domain.item.service;

import com.itssum.domain.item.dto.request.CreateDonationItemRequest;
import com.itssum.domain.item.dto.response.DonationItemDetailResponse;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.service.AddressGeocoder;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.kakao.GeoPoint;
import java.util.HashSet;
import java.util.List;
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
