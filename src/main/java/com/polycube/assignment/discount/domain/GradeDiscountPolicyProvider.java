package com.polycube.assignment.discount.domain;

import com.polycube.assignment.member.domain.MemberGrade;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class GradeDiscountPolicyProvider {

    private final List<DiscountPolicy> policies;

    public GradeDiscountPolicyProvider(List<DiscountPolicy> policies) {
        this.policies = List.copyOf(Objects.requireNonNull(policies, "할인 정책 목록은 필수입니다"));
    }

    public DiscountPolicy get(MemberGrade grade) {
        Objects.requireNonNull(grade, "회원 등급은 필수입니다");
        List<DiscountPolicy> supportedPolicies = policies.stream()
                .filter(policy -> policy.supports(grade))
                .toList();
        if (supportedPolicies.size() != 1) {
            throw new IllegalStateException("회원 등급에 맞는 할인 정책은 정확히 하나여야 합니다");
        }
        return supportedPolicies.get(0);
    }
}
