package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.NeededItem;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NeededItemRepository extends JpaRepository<NeededItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NeededItem n where n.id = :id")
    Optional<NeededItem> findByIdForUpdate(@Param("id") Long id);

    /** 회원 탈퇴 시 그 기관의 필요 물품을 목록에서 내린다 (soft delete). 바꾼 행 수를 돌려준다. */
    @Modifying
    @Query("update NeededItem n set n.deletedAt = :now where n.organization.id = :organizationId and n.deletedAt is null")
    int softDeleteAllByOrganizationId(@Param("organizationId") Long organizationId, @Param("now") LocalDateTime now);

    /**
     * 필요 물품 탐색: 삭제되지 않은 모집중 물품 중 사업자 확인이 유효한(VERIFIED) 기관의 것만, 최신 등록순.
     * 조건이 null 이면 걸지 않는다. likePattern 은 ItemSearchCondition.likePattern() 값이다 (ESCAPE '!').
     */
    @Query("select n from NeededItem n join fetch n.organization o join fetch n.region join fetch n.category "
            + "where n.deletedAt is null and n.status = com.itssum.domain.item.entity.NeededItemStatus.RECRUITING "
            + "and o.verificationStatus = com.itssum.domain.user.entity.VerificationStatus.VERIFIED "
            + "and (:beforeId is null or n.id < :beforeId) "
            + "and (:categoryId is null or n.category.id = :categoryId) "
            + "and (:regionId is null or n.region.id = :regionId) "
            + "and (:condition is null or n.desiredCondition = :condition) "
            + "and (:likePattern is null or n.itemName like :likePattern escape '!' "
            + "or o.name like :likePattern escape '!') "
            + "order by n.id desc")
    List<NeededItem> search(@Param("likePattern") String likePattern, @Param("categoryId") Long categoryId,
                            @Param("regionId") Long regionId, @Param("condition") ConditionGrade condition,
                            @Param("beforeId") Long beforeId, Pageable pageable);

    /** 기관 「필요 나눔 관리」: 상태와 관계없이 삭제되지 않은 우리 기관 물품, 최신 등록순. */
    @Query("select n from NeededItem n join fetch n.category "
            + "where n.organization.id = :organizationId and n.deletedAt is null "
            + "and (:categoryId is null or n.category.id = :categoryId) "
            + "and (:beforeId is null or n.id < :beforeId) "
            + "order by n.id desc")
    List<NeededItem> findMine(@Param("organizationId") Long organizationId, @Param("categoryId") Long categoryId,
                              @Param("beforeId") Long beforeId, Pageable pageable);
}
