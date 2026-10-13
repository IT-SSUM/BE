package com.itssum.domain.user.repository;

import com.itssum.domain.user.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
