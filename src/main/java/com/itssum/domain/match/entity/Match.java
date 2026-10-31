package com.itssum.domain.match.entity;

import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 기부 제안(기부자 → 기관) / 기부 요청(기관 → 기부자). donor_id, organization_id 는 목록 조회용 역정규화 컬럼이다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "matches",
        comment = "매칭",
        indexes = {
                @Index(name = "IDX_matches_donor_status", columnList = "donor_id, status"),
                @Index(name = "IDX_matches_org_status", columnList = "organization_id, status")
        }
)
public class Match extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id", comment = "매칭 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_donation_items_TO_matches"), comment = "기부 물품")
    private DonationItem donationItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "needed_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_needed_items_TO_matches"),
            comment = "필요 물품 (제안·요청 모두 필요 물품에 연결)")
    private NeededItem neededItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_donors_TO_matches"), comment = "기부자 (목록 조회용 역정규화)")
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_organizations_TO_matches"), comment = "기관 (목록 조회용 역정규화)")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, comment = "DONOR(기부 제안) | ORGANIZATION(기부 요청)")
    private RequesterType requestedBy;

    @Column(nullable = false, comment = "제안/요청 수량 (기부자가 예약중으로 변경하면 양쪽 reserved_quantity에 반영)")
    private int quantity;

    @Column(length = 500, comment = "상대에게 전할 메시지")
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'PENDING'")
    @Column(nullable = false, length = 20,
            comment = "PENDING(응답 대기) | TRADING(거래중: 수량 미확보) | RESERVED(예약중: 임시 수량 확보)"
                    + " | COMPLETED(전달 완료: 기관 수령 확인) | REJECTED | CANCELED")
    private MatchStatus status = MatchStatus.PENDING;

    @Column(secondPrecision = 0, comment = "수락·거절 일시")
    private LocalDateTime respondedAt;

    @Column(secondPrecision = 0, comment = "예약중 변경 일시")
    private LocalDateTime reservedAt;

    @Column(secondPrecision = 0, comment = "수령 확인 일시")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20, comment = "전달 방법: DIRECT(직접 전달) | PARCEL(택배) — 채팅에서 협의 후 확정")
    private DeliveryMethod deliveryMethod;

    @Column(secondPrecision = 0, comment = "전달 일정 (전달 약속 · 10월 9일 오후 2시)")
    private LocalDateTime scheduledAt;

    @Column(length = 255, comment = "전달 장소 (구로지역아동센터 입구)")
    private String deliveryPlace;

    @Column(length = 30, comment = "수령 담당자")
    private String receiverName;

    @Column(secondPrecision = 0, comment = "취소 일시")
    private LocalDateTime canceledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "canceled_by", foreignKey = @ForeignKey(name = "FK_users_TO_matches"), comment = "취소한 회원")
    private User canceledBy;

    @Column(length = 255, comment = "취소 사유")
    private String cancelReason;

    @Builder
    private Match(DonationItem donationItem, NeededItem neededItem, Donor donor, Organization organization,
                  RequesterType requestedBy, int quantity, String message) {
        this.donationItem = donationItem;
        this.neededItem = neededItem;
        this.donor = donor;
        this.organization = organization;
        this.requestedBy = requestedBy;
        this.quantity = quantity;
        this.message = message;
    }

    public void accept() {
        this.status = MatchStatus.TRADING;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = MatchStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reserve() {
        this.status = MatchStatus.RESERVED;
        this.reservedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = MatchStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel(User actor, String reason) {
        this.status = MatchStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.canceledBy = actor;
        this.cancelReason = reason;
    }

    public void confirmAppointment(DeliveryMethod method, LocalDateTime dateTime, String place, String receiver) {
        this.deliveryMethod = method;
        this.scheduledAt = dateTime;
        this.deliveryPlace = place;
        this.receiverName = receiver;
    }
}
