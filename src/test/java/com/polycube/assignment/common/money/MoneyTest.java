package com.polycube.assignment.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("음수 금액은 생성할 수 없다")
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Money.of(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("차감한 금액은 0원보다 작아질 수 없다")
    void keepsAmountAtZeroWhenSubtractionExceedsBalance() {
        assertThat(Money.of(500).subtract(Money.of(1_000)))
                .isEqualTo(Money.zero());
    }

    @Test
    @DisplayName("금액을 차감한다")
    void subtractsAmount() {
        assertThat(Money.of(10_000).subtract(Money.of(1_000)))
                .isEqualTo(Money.of(9_000));
    }

    @Test
    @DisplayName("비율 금액의 1원 미만은 버린다")
    void roundsDownFractionalPercentageAmount() {
        assertThat(Money.of(10_005).percentage(new BigDecimal("0.10")))
                .isEqualTo(Money.of(1_000));
    }

    @Test
    @DisplayName("음수 할인율은 적용할 수 없다")
    void rejectsNegativeRate() {
        assertThatThrownBy(() -> Money.of(10_000).percentage(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("100%를 초과한 할인율은 적용할 수 없다")
    void rejectsRateGreaterThanOneHundredPercent() {
        assertThatThrownBy(() -> Money.of(10_000).percentage(new BigDecimal("1.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 금액은 동등하다")
    void considersSameAmountsEqual() {
        assertThat(Money.of(1_000))
                .isEqualTo(Money.of(1_000));
    }
}
