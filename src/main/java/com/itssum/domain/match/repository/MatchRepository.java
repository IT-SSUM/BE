package com.itssum.domain.match.repository;

import com.itssum.domain.match.entity.Match;
import jakarta.persistence.LockModeType;
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

    @EntityGraph(attributePaths = {"donationItem", "neededItem", "donor", "organization"})
    @Query("select m from Match m where m.donor.id = :userId or m.organization.id = :userId order by m.createdAt desc, m.id desc")
    List<Match> findAllForUser(@Param("userId") Long userId);
}
