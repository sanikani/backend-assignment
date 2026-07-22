package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.payment.domain.PaymentMethod;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public final class AppliedDiscount {

    private final DiscountSource source;
    private final String target;
    private final String policyName;
    private final DiscountType discountType;
    private final BigDecimal discountValue;

    @Getter(AccessLevel.NONE)
    private final DiscountResult discountResult;

    private AppliedDiscount(GradeDiscountPolicy policy, DiscountResult discountResult) {
        Objects.requireNonNull(policy, "등급 할인 정책은 필수입니다.");
        this.discountResult = Objects.requireNonNull(
                discountResult,
                "할인 계산 결과는 필수입니다."
        );
        this.source = DiscountSource.GRADE;
        this.target = policy.getMemberGrade().name();
        this.policyName = policy.getName();
        this.discountType = policy.getDiscountType();
        this.discountValue = policy.getDiscountValue();
    }

    private AppliedDiscount(
            String policyName,
            BigDecimal discountRate,
            DiscountResult discountResult
    ) {
        this.policyName = Objects.requireNonNull(policyName, "결제 수단 할인 정책명은 필수입니다.");
        this.discountValue = Objects.requireNonNull(discountRate, "결제 수단 할인율은 필수입니다.");
        this.discountResult = Objects.requireNonNull(
                discountResult,
                "할인 계산 결과는 필수입니다."
        );
        this.source = DiscountSource.PAYMENT_METHOD;
        this.target = PaymentMethod.POINT.name();
        this.discountType = DiscountType.RATE;
    }

    public static AppliedDiscount forGrade(
            GradeDiscountPolicy policy,
            DiscountResult discountResult
    ) {
        return new AppliedDiscount(policy, discountResult);
    }

    public static AppliedDiscount forPoint(
            String policyName,
            BigDecimal discountRate,
            DiscountResult discountResult
    ) {
        return new AppliedDiscount(policyName, discountRate, discountResult);
    }

    public Money getBaseAmount() {
        return discountResult.getOriginalAmount();
    }

    public Money getDiscountAmount() {
        return discountResult.getDiscountAmount();
    }

    public Money getFinalAmount() {
        return discountResult.getFinalAmount();
    }
}
