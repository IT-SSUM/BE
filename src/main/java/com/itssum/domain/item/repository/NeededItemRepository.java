package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.NeededItem;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NeededItemRepository extends JpaRepository<NeededItem, Long> {

    /** 회원 탈퇴 시 그 기관의 필요 물품을 목록에서 내린다 (soft delete). 바꾼 행 수를 돌려준다. */
    @Modifying
    @Query("update NeededItem n set n.deletedAt = :now where n.organization.id = :organizationId and n.deletedAt is null")
    int softDeleteAllByOrganizationId(@Param("organizationId") Long organizationId, @Param("now") LocalDateTime now);
}
