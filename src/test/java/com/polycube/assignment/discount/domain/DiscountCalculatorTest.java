package com.polycube.assignment.discount.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.polycube.assignment.common.money.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DiscountCalculatorTest {

    private final DiscountCalculatorProvider provider = new DiscountCalculatorProvider(List.of(
            new NoDiscountCalculator(),
            new FixedDiscountCalculator(),
            new RateDiscountCalculator()
    ));

    @ParameterizedTest
    @DisplayName("할인 유형에 따라 할인 금액과 최종 금액을 계산한다")
    @CsvSource({
            "NONE, 10000, 0, 0, 10000",
            "FIXED, 500, 1000, 500, 0",
            "RATE, 10005, 0.10, 1000, 9005"
    })
    void calculatesAmountByDiscountType(
            DiscountType discountType,
            long baseAmount,
            String discountValue,
            long expectedDiscountAmount,
            long expectedFinalAmount
    ) {
        DiscountResult result = provider.get(discountType).calculate(
                Money.of(baseAmount),
                new BigDecimal(discountValue)
        );

        assertThat(result.getDiscountAmount()).isEqualTo(Money.of(expectedDiscountAmount));
        assertThat(result.getFinalAmount()).isEqualTo(Money.of(expectedFinalAmount));
    }

    @Test
    @DisplayName("동일한 할인 유형을 지원하는 계산기가 중복되면 예외가 발생한다")
    void rejectsDuplicateCalculatorsForSameType() {
        DiscountCalculatorProvider duplicateProvider = new DiscountCalculatorProvider(List.of(
                new RateDiscountCalculator(),
                new RateDiscountCalculator()
        ));

        assertThatThrownBy(() -> duplicateProvider.get(DiscountType.RATE))
                .isInstanceOf(IllegalStateException.class);
    }
}
