package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VipFixedDiscountPolicyTest {

    private final DiscountPolicy policy = new VipFixedDiscountPolicy();

    @Test
    @DisplayName("VIP 회원은 1,000원을 할인받는다")
    void discountsFixedAmount() {
        DiscountContext context = new DiscountContext(Money.of(10_000), MemberGrade.VIP);

        DiscountResult result = policy.apply(context);

        assertThat(result.getDiscountAmount()).isEqualTo(Money.of(1_000));
        assertThat(result.getFinalAmount()).isEqualTo(Money.of(9_000));
    }

    @Test
    @DisplayName("VIP 할인은 주문 원가를 초과할 수 없다")
    void limitsDiscountToOriginalAmount() {
        DiscountContext context = new DiscountContext(Money.of(500), MemberGrade.VIP);

        DiscountResult result = policy.apply(context);

        assertThat(result.getDiscountAmount()).isEqualTo(Money.of(500));
        assertThat(result.getFinalAmount()).isEqualTo(Money.zero());
    }
}
