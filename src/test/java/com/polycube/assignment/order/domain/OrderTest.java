package com.polycube.assignment.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.Member;
import com.polycube.assignment.member.domain.MemberGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    private final Member member = Member.create(MemberGrade.VIP);

    @Test
    @DisplayName("주문은 상품명과 원가 및 회원 등급을 가지고 생성된다")
    void createsOrder() {
        Order order = Order.create("keyboard", Money.of(10_000), member);

        assertThat(order.getProductName()).isEqualTo("keyboard");
        assertThat(order.getOriginalAmount()).isEqualTo(Money.of(10_000));
        assertThat(order.memberGrade()).isEqualTo(MemberGrade.VIP);
    }

    @Test
    @DisplayName("빈 상품명으로 주문을 생성할 수 없다")
    void rejectsBlankProductName() {
        assertThatThrownBy(() -> Order.create(" ", Money.of(10_000), member))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("주문 원가는 필수다")
    void rejectsMissingOriginalAmount() {
        assertThatThrownBy(() -> Order.create("keyboard", null, member))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("주문 회원은 필수다")
    void rejectsMissingMember() {
        assertThatThrownBy(() -> Order.create("keyboard", Money.of(10_000), null))
                .isInstanceOf(NullPointerException.class);
    }
}
