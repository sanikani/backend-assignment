package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public final class RateDiscountCalculator implements DiscountCalculator {

    @Override
    public boolean supports(DiscountType discountType) {
        return discountType == DiscountType.RATE;
    }

    @Override
    public DiscountResult calculate(Money baseAmount, BigDecimal discountValue) {
        return DiscountResult.of(baseAmount, baseAmount.percentage(discountValue));
    }
}
