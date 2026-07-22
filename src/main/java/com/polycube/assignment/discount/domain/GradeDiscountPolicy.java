package com.polycube.assignment.discount.domain;

import com.polycube.assignment.member.domain.MemberGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "grade_discount_policies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grade_discount_policies_member_grade",
                columnNames = "member_grade"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GradeDiscountPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_grade", nullable = false)
    private MemberGrade memberGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false, precision = 23, scale = 4)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private boolean active;

    private GradeDiscountPolicy(
            String name,
            MemberGrade memberGrade,
            DiscountType discountType,
            BigDecimal discountValue
    ) {
        validateName(name);
        Objects.requireNonNull(memberGrade, "회원 등급은 필수입니다.");
        Objects.requireNonNull(discountType, "할인 타입은 필수입니다.");
        Objects.requireNonNull(discountValue, "할인 값은 필수입니다.");
        discountType.validate(discountValue);
        this.name = name;
        this.memberGrade = memberGrade;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.active = true;
    }

    public static GradeDiscountPolicy create(
            String name,
            MemberGrade memberGrade,
            DiscountType discountType,
            BigDecimal discountValue
    ) {
        return new GradeDiscountPolicy(name, memberGrade, discountType, discountValue);
    }

    public void update(
            String name,
            DiscountType discountType,
            BigDecimal discountValue
    ) {
        validateName(name);
        Objects.requireNonNull(discountType, "할인 타입은 필수입니다.");
        Objects.requireNonNull(discountValue, "할인 값은 필수입니다.");
        discountType.validate(discountValue);
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("할인 정책명은 필수입니다.");
        }
    }
}
