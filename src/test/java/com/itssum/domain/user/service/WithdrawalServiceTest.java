package com.itssum.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.itssum.domain.user.entity.UserStatus;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WithdrawalServiceTest {

    /** 끝난 매칭: 전달 완료·거절·거래 취소. 이 밖의 상태가 하나라도 있으면 탈퇴를 막는다. */
    private static final Set<MatchStatus> FINISHED =
            EnumSet.of(MatchStatus.COMPLETED, MatchStatus.REJECTED, MatchStatus.CANCELED);

    private final MemberReader memberReader = mock(MemberReader.class);
    private final DonorRepository donorRepository = mock(DonorRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final MatchRepository matchRepository = mock(MatchRepository.class);
    private final DonationItemRepository donationItemRepository = mock(DonationItemRepository.class);
    private final NeededItemRepository neededItemRepository = mock(NeededItemRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final PushTokenRepository pushTokenRepository = mock(PushTokenRepository.class);
    private final WithdrawalService service = new WithdrawalService(memberReader, donorRepository,
            organizationRepository, matchRepository, donationItemRepository, neededItemRepository,
            refreshTokenRepository, passwordResetTokenRepository, pushTokenRepository);

    @Test
    void donorWithdrawalAnonymizesAndRevokesEverything() {
        User user = user(1L, UserRole.DONOR);
        Donor donor = donor(user);
        when(memberReader.activeUser(1L)).thenReturn(user);
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));

        service.withdraw(1L);

        verify(matchRepository).existsByDonorIdAndStatusNotIn(1L, FINISHED);
        verify(donationItemRepository).softDeleteAllByDonorId(eq(1L), any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteAllByUserId(1L);
        verify(passwordResetTokenRepository).deleteAllByUserId(1L);
        verify(pushTokenRepository).deleteAllByUserId(1L);
        verifyNoInteractions(neededItemRepository);
        assertEquals("탈퇴한 회원", donor.getName());
        assertEquals(UserStatus.WITHDRAWN, user.getStatus());
        assertEquals("withdrawn-1@itssum.invalid", user.getEmail());
    }

    @Test
    void organizationWithdrawalAnonymizesAndHidesNeededItems() {
        User user = user(2L, UserRole.ORGANIZATION);
        Organization organization = organization(user);
        when(memberReader.activeUser(2L)).thenReturn(user);
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(organization));

        service.withdraw(2L);

        verify(matchRepository).existsByOrganizationIdAndStatusNotIn(2L, FINISHED);
        verify(neededItemRepository).softDeleteAllByOrganizationId(eq(2L), any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteAllByUserId(2L);
        verifyNoInteractions(donationItemRepository);
        assertEquals("탈퇴한 기관", organization.getName());
        assertEquals("W000000002", organization.getBusinessNumber());
        assertEquals(UserStatus.WITHDRAWN, user.getStatus());
    }

    @Test
    void withdrawalBlockedWhileUnfinishedMatchExists() {
        User user = user(1L, UserRole.DONOR);
        Donor donor = donor(user);
        when(memberReader.activeUser(1L)).thenReturn(user);
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(matchRepository.existsByDonorIdAndStatusNotIn(1L, FINISHED)).thenReturn(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.withdraw(1L));

        assertEquals(ErrorCode.USER_WITHDRAWAL_BLOCKED, e.getErrorCode());
        verifyNoInteractions(donationItemRepository, refreshTokenRepository, passwordResetTokenRepository,
                pushTokenRepository);
        assertEquals("최윤서", donor.getName());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    private static User user(Long id, UserRole role) {
        User user = User.builder().email("member@example.com").password("hashed").role(role).build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Donor donor(User user) {
        Donor donor = Donor.builder().user(user).name("최윤서").birthDate(LocalDate.of(1999, 3, 2))
                .phone("010-1234-5678").build();
        ReflectionTestUtils.setField(donor, "id", user.getId());
        return donor;
    }

    private static Organization organization(User user) {
        Organization organization = Organization.builder().user(user).name("구로지역아동센터").representativeName("김수현")
                .businessNumber("1234567890").openingDate(LocalDate.of(2015, 3, 2)).phone("02-1234-5678")
                .address("서울 구로구 가마산로 12").verifiedAt(LocalDateTime.of(2026, 10, 31, 10, 0)).build();
        ReflectionTestUtils.setField(organization, "id", user.getId());
        return organization;
    }
}
