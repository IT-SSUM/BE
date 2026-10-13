package com.itssum.domain.user.repository;

import com.itssum.domain.user.entity.OrganizationVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationVerificationRepository extends JpaRepository<OrganizationVerification, Long> {
}
