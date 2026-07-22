package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.member.domain.MemberGrade;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GradeDiscountPolicyTest {

    @Test
    @DisplayName("등급 할인 정책은 정책명과 등급 및 할인 정보를 가지고 생성된다")
    void createsFixedPolicy() {
        GradeDiscountPolicy policy = GradeDiscountPolicy.create(
                "VIP_FIXED",
                MemberGrade.VIP,
                DiscountType.FIXED,
                new BigDecimal("1000")
        );

        assertThat(policy.getName()).isEqualTo("VIP_FIXED");
        assertThat(policy.getMemberGrade()).isEqualTo(MemberGrade.VIP);
        assertThat(policy.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(policy.getDiscountValue()).isEqualByComparingTo("1000");
        assertThat(policy.isActive()).isTrue();
    }

    @Test
    @DisplayName("정책 내용과 활성 상태를 변경해도 적용 등급은 유지된다")
    void updatesAndChangesActiveState() {
        GradeDiscountPolicy policy = GradeDiscountPolicy.create(
                "VIP_FIXED_V1",
                MemberGrade.VIP,
                DiscountType.FIXED,
                new BigDecimal("1000")
        );

        policy.update("VIP_FIXED_V2", DiscountType.FIXED, new BigDecimal("1500"));
        policy.deactivate();

        assertThat(policy.getName()).isEqualTo("VIP_FIXED_V2");
        assertThat(policy.getMemberGrade()).isEqualTo(MemberGrade.VIP);
        assertThat(policy.getDiscountValue()).isEqualByComparingTo("1500");
        assertThat(policy.isActive()).isFalse();

        policy.activate();
        assertThat(policy.isActive()).isTrue();
    }
}
