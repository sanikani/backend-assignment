package com.polycube.assignment.discount.domain;

import java.math.BigDecimal;

public enum DiscountType {

    NONE {
        @Override
        void validate(BigDecimal value) {
            if (value.compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException(
                        "할인이 없는 정책의 할인 값은 0이어야 합니다."
                );
            }
        }
    },

    FIXED {
        @Override
        void validate(BigDecimal value) {
            if (value.signum() < 0) {
                throw new IllegalArgumentException(
                        "고정 할인 금액은 음수일 수 없습니다."
                );
            }
            if (value.remainder(BigDecimal.ONE).signum() != 0) {
                throw new IllegalArgumentException(
                        "고정 할인 금액은 정수여야 합니다."
                );
            }
            if (value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException(
                        "고정 할인 금액이 허용 범위를 초과했습니다."
                );
            }
        }
    },

    RATE {
        @Override
        void validate(BigDecimal value) {
            if (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException(
                        "비율 할인 값은 0 이상 1 이하여야 합니다."
                );
            }
        }
    };

    abstract void validate(BigDecimal value);
}
