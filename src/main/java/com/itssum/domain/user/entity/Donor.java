package com.itssum.domain.user.entity;

import com.itssum.domain.item.entity.Region;
import com.itssum.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "donors", comment = "기부자")
public class Donor extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    /** @MapsId 라서 user_id 컬럼 정의(COMMENT 포함)는 이 @JoinColumn 을 따른다. */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",
            foreignKey = @ForeignKey(name = "FK_users_TO_donors"), comment = "회원 ID (users PK 공유)")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "region_id", foreignKey = @ForeignKey(name = "FK_regions_TO_donors"),
            comment = "활동 지역 (선택, 마이페이지 \"최윤서님 · 서울 구로구\")")
    private Region region;

    @Column(nullable = false, length = 30, comment = "이름")
    private String name;

    @Column(nullable = false, comment = "생년월일")
    private LocalDate birthDate;

    @Column(nullable = false, length = 20, comment = "전화번호")
    private String phone;

    @Column(length = 255, comment = "기부자 소개 (기관 화면의 기부 물품 상세에 노출)")
    private String introduction;

    @Builder
    private Donor(User user, Region region, String name, LocalDate birthDate, String phone, String introduction) {
        this.user = user;
        this.region = region;
        this.name = name;
        this.birthDate = birthDate;
        this.phone = phone;
        this.introduction = introduction;
    }

    /** 회원정보 수정. 이메일·활동 지역은 바꾸지 않는다. */
    public void updateProfile(String name, LocalDate birthDate, String phone) {
        this.name = name;
        this.birthDate = birthDate;
        this.phone = phone;
    }

    /**
     * 탈퇴 시 개인정보를 지운다. 이름·생년월일·전화번호는 NOT NULL 이라 자리표시 값으로 덮어쓰고,
     * 상대방의 나눔 내역에는 "탈퇴한 회원"으로 보인다.
     */
    public void anonymize() {
        this.name = "탈퇴한 회원";
        this.birthDate = LocalDate.of(1900, 1, 1);
        this.phone = "000-0000-0000";
        this.introduction = null;
    }
}
