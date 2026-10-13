package com.itssum.domain.user.repository;

import com.itssum.domain.user.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonorRepository extends JpaRepository<Donor, Long> {
}
