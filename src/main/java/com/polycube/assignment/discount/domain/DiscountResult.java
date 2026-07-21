package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class DiscountResult {

    private final Money originalAmount;
    private final Money discountAmount;
    private final Money finalAmount;

    private DiscountResult(Money originalAmount, Money requestedDiscountAmount) {
        this.originalAmount = Objects.requireNonNull(originalAmount, "주문 원가는 필수입니다");
        Objects.requireNonNull(requestedDiscountAmount, "할인 금액은 필수입니다");
        this.finalAmount = originalAmount.subtract(requestedDiscountAmount);
        this.discountAmount = Money.of(originalAmount.getValue() - finalAmount.getValue());
    }

    public static DiscountResult of(Money originalAmount, Money discountAmount) {
        return new DiscountResult(originalAmount, discountAmount);
    }
}
