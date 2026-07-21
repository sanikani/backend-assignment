package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.polycube.assignment.member.domain.MemberGrade;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GradeDiscountPolicyProviderTest {

    private final GradeDiscountPolicyProvider policyProvider = new GradeDiscountPolicyProvider(List.of(
            new NormalDiscountPolicy(),
            new VipFixedDiscountPolicy(),
            new VvipRateDiscountPolicy()
    ));

    @ParameterizedTest
    @EnumSource(MemberGrade.class)
    @DisplayName("모든 회원 등급에는 정확히 하나의 할인 정책이 존재한다")
    void hasExactlyOnePolicyForEveryMemberGrade(MemberGrade grade) {
        assertThatCode(() -> policyProvider.get(grade))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("지원하는 할인 정책이 없으면 실패한다")
    void rejectsMissingPolicy() {
        GradeDiscountPolicyProvider emptyProvider = new GradeDiscountPolicyProvider(List.of());

        assertThatThrownBy(() -> emptyProvider.get(MemberGrade.VIP))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("같은 등급을 지원하는 정책이 여러 개면 실패한다")
    void rejectsDuplicatePolicies() {
        GradeDiscountPolicyProvider duplicateProvider = new GradeDiscountPolicyProvider(List.of(
                new VipFixedDiscountPolicy(),
                new VipFixedDiscountPolicy()
        ));

        assertThatThrownBy(() -> duplicateProvider.get(MemberGrade.VIP))
                .isInstanceOf(IllegalStateException.class);
    }
}
