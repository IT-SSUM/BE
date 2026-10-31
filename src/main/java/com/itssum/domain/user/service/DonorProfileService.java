package com.itssum.domain.user.service;

import com.itssum.domain.user.dto.request.UpdateDonorProfileRequest;
import com.itssum.domain.user.dto.response.DonorProfileResponse;
import com.itssum.domain.user.entity.Donor;
import com.itssum.global.common.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DonorProfileService {

    private final MemberReader memberReader;

    @Transactional(readOnly = true)
    public DonorProfileResponse get(Long userId) {
        return DonorProfileResponse.from(memberReader.activeDonor(userId));
    }

    /** 보내지 않은 필드(null)는 그대로 둔다. 같은 회원의 탈퇴와 겹치지 않도록 회원 행을 잠그고 읽는다. */
    @Transactional
    public DonorProfileResponse update(Long userId, UpdateDonorProfileRequest request) {
        Donor donor = memberReader.lockActiveDonor(userId);
        donor.updateProfile(
                request.name() == null ? donor.getName() : request.name().strip(),
                request.birthDate() == null ? donor.getBirthDate() : request.birthDate(),
                request.phone() == null ? donor.getPhone() : PhoneNumbers.format(request.phone()));
        return DonorProfileResponse.from(donor);
    }
}
