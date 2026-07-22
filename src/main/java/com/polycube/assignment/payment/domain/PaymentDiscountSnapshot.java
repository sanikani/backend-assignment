package com.polycube.assignment.payment.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.discount.domain.AppliedDiscount;
import com.polycube.assignment.discount.domain.DiscountSource;
import com.polycube.assignment.discount.domain.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentDiscountSnapshot {

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountSource source;

    @Getter
    @Column(nullable = false)
    private String target;

    @Getter
    @Column(nullable = false)
    private String policyName;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Getter
    @Column(nullable = false, precision = 23, scale = 4)
    private BigDecimal discountValue;

    @Column(nullable = false)
    private long baseAmount;

    @Column(nullable = false)
    private long discountAmount;

    @Column(nullable = false)
    private long finalAmount;

    private PaymentDiscountSnapshot(AppliedDiscount appliedDiscount) {
        Objects.requireNonNull(appliedDiscount, "적용된 할인은 필수입니다.");
        this.source = appliedDiscount.getSource();
        this.target = appliedDiscount.getTarget();
        this.policyName = appliedDiscount.getPolicyName();
        this.discountType = appliedDiscount.getDiscountType();
        this.discountValue = appliedDiscount.getDiscountValue();
        this.baseAmount = appliedDiscount.getBaseAmount().getValue();
        this.discountAmount = appliedDiscount.getDiscountAmount().getValue();
        this.finalAmount = appliedDiscount.getFinalAmount().getValue();
    }

    public static PaymentDiscountSnapshot from(AppliedDiscount appliedDiscount) {
        return new PaymentDiscountSnapshot(appliedDiscount);
    }

    public Money getBaseAmount() {
        return Money.of(baseAmount);
    }

    public Money getDiscountAmount() {
        return Money.of(discountAmount);
    }

    public Money getFinalAmount() {
        return Money.of(finalAmount);
    }
}
