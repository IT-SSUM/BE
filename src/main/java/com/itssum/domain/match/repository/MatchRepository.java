package com.itssum.domain.match.repository;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Match m where m.id = :id")
    Optional<Match> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"donationItem", "donationItem.category", "donationItem.region",
            "neededItem", "neededItem.region", "donor", "organization"})
    @Query("select m from Match m where m.donor.id = :userId or m.organization.id = :userId order by m.createdAt desc, m.id desc")
    List<Match> findAllForUser(@Param("userId") Long userId);

    /** 회원 탈퇴 검사용: 주어진 상태(끝난 상태)가 아닌 매칭이 남아 있는지 */
    boolean existsByDonorIdAndStatusNotIn(Long donorId, Collection<MatchStatus> statuses);

    boolean existsByOrganizationIdAndStatusNotIn(Long organizationId, Collection<MatchStatus> statuses);
}
