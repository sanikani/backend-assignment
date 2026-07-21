package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import org.springframework.stereotype.Component;

@Component
public final class VipFixedDiscountPolicy implements DiscountPolicy {

    private static final Money DISCOUNT_AMOUNT = Money.of(1_000);

    @Override
    public boolean supports(MemberGrade grade) {
        return grade == MemberGrade.VIP;
    }

    @Override
    public DiscountResult apply(DiscountContext context) {
        return DiscountResult.of(context.getOriginalAmount(), DISCOUNT_AMOUNT);
    }
}
