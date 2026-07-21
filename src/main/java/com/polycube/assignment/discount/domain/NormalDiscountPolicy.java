package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import org.springframework.stereotype.Component;

@Component
public final class NormalDiscountPolicy implements DiscountPolicy {

    @Override
    public boolean supports(MemberGrade grade) {
        return grade == MemberGrade.NORMAL;
    }

    @Override
    public DiscountResult apply(DiscountContext context) {
        return DiscountResult.of(context.getOriginalAmount(), Money.zero());
    }
}
