package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import java.math.BigDecimal;
import java.util.Objects;

public final class PointPaymentDiscountPolicy {

    private static final String POLICY_NAME = "POINT_RATE_5_PERCENT";
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05");

    public AppliedDiscount apply(Money baseAmount) {
        Objects.requireNonNull(baseAmount, "할인 적용 전 금액은 필수입니다.");
        DiscountResult result = DiscountResult.of(
                baseAmount,
                baseAmount.percentage(DISCOUNT_RATE)
        );
        return AppliedDiscount.forPoint(POLICY_NAME, DISCOUNT_RATE, result);
    }
}
