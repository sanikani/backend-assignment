package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PointPaymentDiscountPolicyTest {

    private final PointPaymentDiscountPolicy policy = new PointPaymentDiscountPolicy();

    @Test
    @DisplayName("포인트 결제는 이전 할인 적용 후 금액에서 5%를 추가 할인한다")
    void appliesFivePercentDiscount() {
        AppliedDiscount appliedDiscount = policy.apply(Money.of(9_000));

        assertThat(appliedDiscount.getSource()).isEqualTo(DiscountSource.PAYMENT_METHOD);
        assertThat(appliedDiscount.getTarget()).isEqualTo("POINT");
        assertThat(appliedDiscount.getPolicyName()).isEqualTo("POINT_RATE_5_PERCENT");
        assertThat(appliedDiscount.getDiscountType()).isEqualTo(DiscountType.RATE);
        assertThat(appliedDiscount.getDiscountValue()).isEqualByComparingTo("0.05");
        assertThat(appliedDiscount.getBaseAmount()).isEqualTo(Money.of(9_000));
        assertThat(appliedDiscount.getDiscountAmount()).isEqualTo(Money.of(450));
        assertThat(appliedDiscount.getFinalAmount()).isEqualTo(Money.of(8_550));
    }
}
