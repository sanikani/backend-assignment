package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import java.math.BigDecimal;

public interface DiscountCalculator {

    boolean supports(DiscountType discountType);

    DiscountResult calculate(Money baseAmount, BigDecimal discountValue);
}
