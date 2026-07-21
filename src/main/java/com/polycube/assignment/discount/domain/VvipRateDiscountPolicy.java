package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public final class VvipRateDiscountPolicy implements DiscountPolicy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public boolean supports(MemberGrade grade) {
        return grade == MemberGrade.VVIP;
    }

    @Override
    public DiscountResult apply(DiscountContext context) {
        Money originalAmount = context.getOriginalAmount();
        return DiscountResult.of(originalAmount, originalAmount.percentage(DISCOUNT_RATE));
    }
}
