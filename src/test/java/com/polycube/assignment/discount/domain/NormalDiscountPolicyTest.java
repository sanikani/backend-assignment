package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NormalDiscountPolicyTest {

    private final DiscountPolicy policy = new NormalDiscountPolicy();

    @Test
    @DisplayName("NORMAL 회원은 할인받지 않는다")
    void doesNotDiscountNormalMember() {
        DiscountContext context = new DiscountContext(Money.of(10_000), MemberGrade.NORMAL);

        DiscountResult result = policy.apply(context);

        assertThat(result.getDiscountAmount()).isEqualTo(Money.zero());
        assertThat(result.getFinalAmount()).isEqualTo(Money.of(10_000));
    }
}
