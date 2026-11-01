package com.itssum.domain.user.service;

import com.itssum.domain.user.dto.request.UpdateOrganizationProfileRequest;
import com.itssum.domain.user.dto.response.OrganizationDetailResponse;
import com.itssum.domain.user.dto.response.OrganizationProfileResponse;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.global.common.PhoneNumbers;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.kakao.GeoPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrganizationProfileService {

    private final MemberReader memberReader;
    private final OrganizationRepository organizationRepository;
    private final AddressGeocoder addressGeocoder;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public OrganizationProfileResponse getMine(Long userId) {
        return OrganizationProfileResponse.from(memberReader.activeOrganization(userId));
    }

    /** 로그인한 회원이면 역할과 상관없이 볼 수 있다. 탈퇴한 기관은 없는 기관처럼 404 로 응답한다. */
    @Transactional(readOnly = true)
    public OrganizationDetailResponse getDetail(Long viewerId, Long organizationId) {
        memberReader.activeUser(viewerId);
        return organizationRepository.findById(organizationId)
                .filter(organization -> organization.getUser().isActive())
                .map(OrganizationDetailResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_ORGANIZATION_NOT_FOUND));
    }

    /**
     * 보내지 않은 필드(null)는 그대로 둔다. 주소와 상세주소는 한 묶음이라 주소를 보내면 상세주소도 이 요청의 값으로 바뀐다.
     * 주소가 바뀌었거나 좌표가 비어 있을 때(가입 때 카카오 장애 등)만 좌표를 다시 계산하고,
     * 카카오 호출 동안 DB 트랜잭션을 잡지 않도록 저장만 트랜잭션으로 묶는다.
     */
    public OrganizationProfileResponse updateMine(Long userId, UpdateOrganizationProfileRequest request) {
        String address = request.address() == null ? null : request.address().strip();
        boolean relocated = address != null && needsGeocoding(address, memberReader.activeOrganization(userId));
        GeoPoint location = relocated ? addressGeocoder.locate(address).orElse(null) : null;

        return transactionTemplate.execute(status -> {
            // 같은 회원의 탈퇴와 겹치지 않도록 저장할 때만 회원 행을 잠근다 (카카오 호출 중에는 잠그지 않음)
            Organization organization = memberReader.lockActiveOrganization(userId);
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

    /** 주소가 바뀌었거나, 같은 주소라도 좌표가 비어 있으면(가입 때 카카오 장애 등) 다시 계산한다. */
    private static boolean needsGeocoding(String address, Organization current) {
        return !address.equals(current.getAddress()) || current.getLatitude() == null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
