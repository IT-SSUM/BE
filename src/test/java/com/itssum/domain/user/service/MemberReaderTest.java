package com.itssum.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.repository.DonorRepository;
import com.itssum.domain.user.repository.OrganizationRepository;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class MemberReaderTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DonorRepository donorRepository = mock(DonorRepository.class);
    private final OrganizationRepository organizationRepository = mock(OrganizationRepository.class);
    private final MemberReader reader = new MemberReader(userRepository, donorRepository, organizationRepository);

    @Test
    void activeUserRejectsMissingAndWithdrawnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        assertUnauthorized(() -> reader.activeUser(1L));

        User withdrawn = user(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(withdrawn));
        assertUnauthorized(() -> reader.activeUser(1L));
    }

    @Test
    void activeDonorRejectsUserWithoutDonorRow() {
        User active = user(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        when(donorRepository.findById(1L)).thenReturn(Optional.empty());

        assertUnauthorized(() -> reader.activeDonor(1L));
    }

    @Test
    void activeDonorAndOrganizationOfActiveUser() {
        Donor donor = mock(Donor.class);
        Organization organization = mock(Organization.class);
        User active = user(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(active));
        when(donorRepository.findById(1L)).thenReturn(Optional.of(donor));
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));

        assertSame(donor, reader.activeDonor(1L));
        assertSame(organization, reader.activeOrganization(1L));
    }

    @Test
    void activeOrganizationRejectsWithdrawnUser() {
        User withdrawn = user(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(withdrawn));
        when(organizationRepository.findById(2L)).thenReturn(Optional.of(mock(Organization.class)));

        assertUnauthorized(() -> reader.activeOrganization(2L));
    }

    private static User user(boolean active) {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(active);
        return user;
    }

    private static void assertUnauthorized(Executable executable) {
        BusinessException e = assertThrows(BusinessException.class, executable);
        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
    }
}
