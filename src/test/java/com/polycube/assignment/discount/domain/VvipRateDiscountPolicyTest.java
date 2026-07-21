package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VvipRateDiscountPolicyTest {

    private final DiscountPolicy policy = new VvipRateDiscountPolicy();

    @Test
    @DisplayName("VVIP 회원은 주문 원가의 10%를 할인받는다")
    void discountsRateAmount() {
        DiscountContext context = new DiscountContext(Money.of(10_000), MemberGrade.VVIP);

        DiscountResult result = policy.apply(context);

        assertThat(result.getDiscountAmount()).isEqualTo(Money.of(1_000));
        assertThat(result.getFinalAmount()).isEqualTo(Money.of(9_000));
    }

    @Test
    @DisplayName("VVIP 할인 금액의 1원 미만은 버린다")
    void roundsDownFractionalAmount() {
        DiscountContext context = new DiscountContext(Money.of(10_005), MemberGrade.VVIP);

        DiscountResult result = policy.apply(context);

        assertThat(result.getDiscountAmount()).isEqualTo(Money.of(1_000));
        assertThat(result.getFinalAmount()).isEqualTo(Money.of(9_005));
    }
}
