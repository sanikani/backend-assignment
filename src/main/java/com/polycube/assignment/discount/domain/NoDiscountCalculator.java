package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public final class NoDiscountCalculator implements DiscountCalculator {

    @Override
    public boolean supports(DiscountType discountType) {
        return discountType == DiscountType.NONE;
    }

    @Override
    public DiscountResult calculate(Money baseAmount, BigDecimal discountValue) {
        return DiscountResult.of(baseAmount, Money.zero());
    }
}
