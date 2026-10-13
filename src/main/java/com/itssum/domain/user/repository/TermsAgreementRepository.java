package com.itssum.domain.user.repository;

import com.itssum.domain.user.entity.TermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {
}
