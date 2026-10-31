package com.itssum.domain.user.service;

import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 토큰의 회원 ID 로 회원을 다시 읽는다. 탈퇴한 회원의 액세스 토큰은 만료(최대 30분) 전까지 서명이 유효하므로,
 * 회원 API 는 이 클래스로 회원 상태를 확인해 401 로 막는다.
 */
@Component
@RequiredArgsConstructor
public class MemberReader {

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final OrganizationRepository organizationRepository;

    public User activeUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public Donor activeDonor(Long userId) {
        activeUser(userId);
        return donorRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    public Organization activeOrganization(Long userId) {
        activeUser(userId);
        return organizationRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
