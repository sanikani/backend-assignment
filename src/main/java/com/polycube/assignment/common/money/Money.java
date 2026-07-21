package com.polycube.assignment.common.money;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Getter
@EqualsAndHashCode
public final class Money {

    private static final Money ZERO = new Money(0);

    private final long value;

    public Money(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다");
        }

        this.value = value;
    }

    public static Money of(long value) {
        if (value == 0) {
            return ZERO;
        }
        return new Money(value);
    }

    public static Money zero() {
        return ZERO;
    }

    public Money subtract(Money money) {
        Objects.requireNonNull(money, "차감액은 필수입니다");
        if (this.value < money.value) {
            return ZERO;
        }
        return new Money(this.value - money.value);
    }

    public Money percentage(BigDecimal rate) {
        Objects.requireNonNull(rate, "할인률은 필수입니다");
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("할인률은 0 이상 1 이하여야 합니다");
        }
        long calculated = BigDecimal.valueOf(this.value)
                .multiply(rate)
                .setScale(0, RoundingMode.DOWN)
                .longValueExact();
        return new Money(calculated);
    }
}
