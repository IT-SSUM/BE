package com.itssum.domain.recommendation.repository;

import com.itssum.domain.recommendation.entity.OrganizationRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRecommendationRepository extends JpaRepository<OrganizationRecommendation, Long> {
}
