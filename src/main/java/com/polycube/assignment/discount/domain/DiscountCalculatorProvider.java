package com.polycube.assignment.discount.domain;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DiscountCalculatorProvider {

    private final List<DiscountCalculator> calculators;

    public DiscountCalculatorProvider(List<DiscountCalculator> calculators) {
        this.calculators = List.copyOf(Objects.requireNonNull(
                calculators,
                "할인 계산기 목록은 필수입니다."
        ));
    }

    public DiscountCalculator get(DiscountType discountType) {
        Objects.requireNonNull(discountType, "할인 유형은 필수입니다.");
        List<DiscountCalculator> supportedCalculators = calculators.stream()
                .filter(calculator -> calculator.supports(discountType))
                .toList();
        if (supportedCalculators.size() != 1) {
            throw new IllegalStateException("할인 유형을 지원하는 계산기는 정확히 하나여야 합니다.");
        }
        return supportedCalculators.get(0);
    }
}
