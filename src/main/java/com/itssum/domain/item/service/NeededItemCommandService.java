package com.itssum.domain.item.service;

import com.itssum.domain.item.dto.request.CreateNeededItemRequest;
import com.itssum.domain.item.dto.response.NeededItemDetailResponse;
import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.image.ItemImageKind;
import com.itssum.domain.item.image.ItemImages;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.service.MemberReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 필요 물품 등록·수정·삭제. 거리는 기관 주소 좌표로 계산하므로 필요 물품은 좌표를 구하지 않는다.
 * S3 업로드 확인은 트랜잭션 밖에서 하고, 저장할 때 기관 행을 잠가 같은 회원의 탈퇴와 겹치지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class NeededItemCommandService {

    private final MemberReader memberReader;
    private final ItemReferences itemReferences;
    private final ItemImages itemImages;
    private final NeededItemRepository neededItemRepository;
    private final TransactionTemplate transactionTemplate;

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
}
