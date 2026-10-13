package com.itssum.domain.recommendation.repository;

import com.itssum.domain.recommendation.entity.DonationItemRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationItemRecommendationRepository extends JpaRepository<DonationItemRecommendation, Long> {
}
