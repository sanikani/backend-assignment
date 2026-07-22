package com.polycube.assignment.common.error;

import lombok.Getter;

@Getter
public enum ErrorCode {
    ORDER_NOT_FOUND("주문을 찾을 수 없습니다."),
    ORDER_ALREADY_PAID("이미 결제된 주문입니다."),
    GRADE_DISCOUNT_POLICY_ALREADY_EXISTS("해당 회원 등급의 할인 정책이 이미 존재합니다."),
    GRADE_DISCOUNT_POLICY_NOT_FOUND("할인 정책을 찾을 수 없습니다.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }
}
