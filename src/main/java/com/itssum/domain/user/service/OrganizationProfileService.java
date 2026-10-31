package com.itssum.domain.user.service;

import com.itssum.domain.user.dto.request.UpdateOrganizationProfileRequest;
import com.itssum.domain.user.dto.response.OrganizationProfileResponse;
import com.itssum.domain.user.entity.Organization;
import com.itssum.global.common.PhoneNumbers;
import com.itssum.global.infra.kakao.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrganizationProfileService {

    private final MemberReader memberReader;
    private final AddressGeocoder addressGeocoder;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public OrganizationProfileResponse getMine(Long userId) {
        return OrganizationProfileResponse.from(memberReader.activeOrganization(userId));
    }

    /**
     * 보내지 않은 필드(null)는 그대로 둔다. 주소와 상세주소는 한 묶음이라 주소를 보내면 상세주소도 이 요청의 값으로 바뀐다.
     * 주소가 실제로 바뀔 때만 좌표를 다시 계산하고, 카카오 호출 동안 DB 트랜잭션을 잡지 않도록 저장만 트랜잭션으로 묶는다.
     */
    public OrganizationProfileResponse updateMine(Long userId, UpdateOrganizationProfileRequest request) {
        String address = request.address() == null ? null : request.address().strip();
        boolean relocated = address != null && !address.equals(memberReader.activeOrganization(userId).getAddress());
        GeoPoint location = relocated ? addressGeocoder.locate(address).orElse(null) : null;

        return transactionTemplate.execute(status -> {
            Organization organization = memberReader.activeOrganization(userId);
            organization.updateProfile(
                    request.name() == null ? organization.getName() : request.name().strip(),
                    request.phone() == null ? organization.getPhone() : PhoneNumbers.format(request.phone()));
            String addressDetail = blankToNull(request.addressDetail());
            if (relocated) {
                organization.relocate(address, addressDetail,
                        location == null ? null : location.latitude(),
                        location == null ? null : location.longitude());
            } else if (address != null || request.addressDetail() != null) {
                organization.changeAddressDetail(addressDetail);
            }
            return OrganizationProfileResponse.from(organization);
        });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
