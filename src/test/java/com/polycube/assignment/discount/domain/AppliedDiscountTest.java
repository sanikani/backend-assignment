package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AppliedDiscountTest {

    @Test
    @DisplayName("적용된 등급 할인은 정책 정보와 계산 결과를 보존한다")
    void preservesGradePolicyAndDiscountResult() {
        GradeDiscountPolicy policy = GradeDiscountPolicy.create(
                "VIP_FIXED",
                MemberGrade.VIP,
                DiscountType.FIXED,
                new BigDecimal("1000")
        );
        DiscountResult result = DiscountResult.of(Money.of(10_000), Money.of(1_000));

        AppliedDiscount appliedDiscount = AppliedDiscount.forGrade(policy, result);
        policy.update("VIP_RATE", DiscountType.RATE, new BigDecimal("0.10"));

        assertThat(appliedDiscount.getSource()).isEqualTo(DiscountSource.GRADE);
        assertThat(appliedDiscount.getTarget()).isEqualTo("VIP");
        assertThat(appliedDiscount.getPolicyName()).isEqualTo("VIP_FIXED");
        assertThat(appliedDiscount.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(appliedDiscount.getDiscountValue()).isEqualByComparingTo("1000");
        assertThat(appliedDiscount.getBaseAmount()).isEqualTo(Money.of(10_000));
        assertThat(appliedDiscount.getDiscountAmount()).isEqualTo(Money.of(1_000));
        assertThat(appliedDiscount.getFinalAmount()).isEqualTo(Money.of(9_000));
    }
}
