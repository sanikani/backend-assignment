package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public final class FixedDiscountCalculator implements DiscountCalculator {

    @Override
    public boolean supports(DiscountType discountType) {
        return discountType == DiscountType.FIXED;
    }

    @Override
    public DiscountResult calculate(Money baseAmount, BigDecimal discountValue) {
        return DiscountResult.of(baseAmount, Money.of(discountValue.longValueExact()));
    }
}
