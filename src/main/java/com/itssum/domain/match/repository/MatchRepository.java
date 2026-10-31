package com.itssum.domain.match.repository;

import com.itssum.domain.match.entity.Match;
import com.itssum.domain.match.entity.MatchStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, Long> {

    /** 회원 탈퇴 검사용: 주어진 상태(끝난 상태)가 아닌 매칭이 남아 있는지 */
    boolean existsByDonorIdAndStatusNotIn(Long donorId, Collection<MatchStatus> statuses);

    boolean existsByOrganizationIdAndStatusNotIn(Long organizationId, Collection<MatchStatus> statuses);
}
