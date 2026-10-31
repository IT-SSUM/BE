package com.itssum.domain.user.service;

import com.itssum.domain.auth.repository.PasswordResetTokenRepository;
import com.itssum.domain.auth.repository.RefreshTokenRepository;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.repository.MatchRepository;
import com.itssum.domain.notification.repository.PushTokenRepository;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴. 진행 중인 거래가 있으면 막고, 없으면 물품을 내리고 모든 기기에서 로그아웃시킨 뒤 개인정보를 지운다.
 * 약관 동의·사업자 확인 이력과 매칭·채팅 기록은 상대방의 기록이기도 해서 남긴다.
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    /**
     * 끝난 매칭: 전달 완료·거절·거래 취소. 이 밖의 상태(응답 대기 중인 제안·요청, 거래중, 예약중)는 진행 중으로 본다.
     * 진행 중 상태의 이름이 바뀌어도 규칙이 그대로 맞도록 끝난 상태로 판단한다.
     */
    private static final Set<MatchStatus> FINISHED_MATCH_STATUSES =
            EnumSet.of(MatchStatus.COMPLETED, MatchStatus.REJECTED, MatchStatus.CANCELED);

    private final MemberReader memberReader;
    private final DonorRepository donorRepository;
    private final OrganizationRepository organizationRepository;
    private final MatchRepository matchRepository;
    private final DonationItemRepository donationItemRepository;
    private final NeededItemRepository neededItemRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PushTokenRepository pushTokenRepository;

    @Transactional
    public void withdraw(Long userId) {
        User user = memberReader.activeUser(userId);
        LocalDateTime now = LocalDateTime.now();
        if (user.getRole() == UserRole.DONOR) {
            Donor donor = donorRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
            if (matchRepository.existsByDonorIdAndStatusNotIn(userId, FINISHED_MATCH_STATUSES)) {
                throw new BusinessException(ErrorCode.USER_WITHDRAWAL_BLOCKED);
            }
            donationItemRepository.softDeleteAllByDonorId(userId, now);
            donor.anonymize();
        } else {
            Organization organization = organizationRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
            if (matchRepository.existsByOrganizationIdAndStatusNotIn(userId, FINISHED_MATCH_STATUSES)) {
                throw new BusinessException(ErrorCode.USER_WITHDRAWAL_BLOCKED);
            }
            neededItemRepository.softDeleteAllByOrganizationId(userId, now);
            organization.anonymize();
        }
        refreshTokenRepository.deleteAllByUserId(userId);
        passwordResetTokenRepository.deleteAllByUserId(userId);
        pushTokenRepository.deleteAllByUserId(userId);
        user.withdraw();
    }
}
