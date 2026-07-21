package com.polycube.assignment.discount.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.MemberGrade;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class DiscountContext {

    private final Money originalAmount;
    private final MemberGrade memberGrade;

    public DiscountContext(Money originalAmount, MemberGrade memberGrade) {
        this.originalAmount = Objects.requireNonNull(originalAmount, "주문 원가는 필수입니다");
        this.memberGrade = Objects.requireNonNull(memberGrade, "회원 등급은 필수입니다");
    }
}
