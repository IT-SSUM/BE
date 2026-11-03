package com.itssum.domain.recommendation.repository;

import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.recommendation.entity.OrganizationRecommendation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRecommendationRepository extends JpaRepository<OrganizationRecommendation, Long> {

    /**
     * 재계산 전에 이 기부 물품의 추천을 지운다. 벌크 삭제라 바로 실행된다.
     * (엔티티 단위로 지우면 Hibernate 가 flush 때 INSERT 를 먼저 실행해 UQ_org_rec_item_needed 에 걸린다.)
     */
    @Modifying
    @Query("delete from OrganizationRecommendation r where r.donationItem.id = :donationItemId")
    int deleteAllByDonationItemId(@Param("donationItemId") Long donationItemId);

    /**
     * 저장된 추천 중 지금 제안할 수 있는 것만 순위순으로: 필요 물품이 삭제되지 않은 모집중이고, 기관이 VERIFIED 이며,
     * 그 쌍에 끝나지 않은 매칭이 없는 것. 필요 물품과 기관을 함께 읽는다.
     */
    @Query("select r from OrganizationRecommendation r join fetch r.neededItem n join fetch r.organization o "
            + "where r.donationItem.id = :donationItemId "
            + "and n.deletedAt is null and n.status = com.itssum.domain.item.entity.NeededItemStatus.RECRUITING "
            + "and o.verificationStatus = com.itssum.domain.user.entity.VerificationStatus.VERIFIED "
            + "and not exists (select m.id from Match m where m.donationItem.id = :donationItemId "
            + "and m.neededItem = n and m.status not in :finished) "
            + "order by r.rankNo")
    List<OrganizationRecommendation> findAvailable(@Param("donationItemId") Long donationItemId,
                                                   @Param("finished") Collection<MatchStatus> finished);
}
