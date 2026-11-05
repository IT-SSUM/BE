package com.itssum.domain.recommendation.service;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.item.repository.DonationItemRepository;
import com.itssum.domain.item.repository.NeededItemRepository;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.recommendation.dto.response.OrganizationRecommendationResponse;
import com.itssum.domain.recommendation.entity.OrganizationRecommendation;
import com.itssum.domain.recommendation.repository.OrganizationRecommendationRepository;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.service.MemberReader;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기부자 「기관 추천」. POST 는 기부 물품 행을 잠그고 추천을 다시 계산해 저장하고(이전 결과는 지운다),
 * GET 은 저장된 추천 중 지금 제안할 수 있는 것만 순위를 다시 매겨 돌려준다.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MemberReader memberReader;
    private final DonationItemRepository donationItemRepository;
    private final NeededItemRepository neededItemRepository;
    private final OrganizationRecommendationRepository recommendationRepository;

    @Transactional
    public List<OrganizationRecommendationResponse> recommend(Long donorId, Long donationItemId) {
        memberReader.activeDonor(donorId);
        DonationItem donation = ownedItem(donationItemRepository.findByIdForUpdate(donationItemId), donorId);
        recommendationRepository.deleteAllByDonationItemId(donationItemId);
        if (donation.availableQuantity() <= 0) {
            return List.of();
        }
        List<NeededItem> neededItems = neededItemRepository.findRecommendationCandidates(donationItemId,
                donation.getCategory().getId(), donation.getUnit(),
                EnumSet.range(donation.getConditionGrade(), ConditionGrade.LOW), MatchStatus.FINISHED);
        List<RecommendationCandidate> candidates = neededItems.stream()
                .map(needed -> new RecommendationCandidate(needed, distanceKm(donation, needed.getOrganization())))
                .toList();
        List<OrganizationRecommendation> saved = recommendationRepository.saveAll(
                RecommendationRanker.rank(donation.availableQuantity(), candidates).stream()
                        .map(ranked -> OrganizationRecommendation.builder().donationItem(donation)
                                .neededItem(ranked.neededItem()).organization(ranked.neededItem().getOrganization())
                                .rankNo(ranked.rank()).score(ranked.score()).distanceKm(ranked.distanceKm())
                                .reason(RecommendationReason.from(donation.availableQuantity(), ranked)).build())
                        .toList());
        return saved.stream()
                .map(recommendation -> OrganizationRecommendationResponse.of(recommendation.getRankNo(), recommendation))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrganizationRecommendationResponse> find(Long donorId, Long donationItemId) {
        memberReader.activeDonor(donorId);
        DonationItem donation = ownedItem(donationItemRepository.findById(donationItemId), donorId);
        if (donation.availableQuantity() <= 0) {
            return List.of();
        }
        List<OrganizationRecommendation> available =
                recommendationRepository.findAvailable(donationItemId, MatchStatus.FINISHED);
        return IntStream.range(0, available.size())
                .mapToObj(index -> OrganizationRecommendationResponse.of(index + 1, available.get(index)))
                .toList();
    }

    private static DonationItem ownedItem(Optional<DonationItem> found, Long donorId) {
        DonationItem donation = found.filter(item -> !item.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));
        if (!donation.isOwnedBy(donorId)) {
            throw new BusinessException(ErrorCode.RECOMMENDATION_NOT_OWNER);
        }
        return donation;
    }

    private static Double distanceKm(DonationItem donation, Organization organization) {
        return RecommendationScorer.distanceKm(donation.getLatitude(), donation.getLongitude(),
                organization.getLatitude(), organization.getLongitude());
    }
}
