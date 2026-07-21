package com.polycube.assignment.order.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.Member;
import com.polycube.assignment.member.domain.MemberGrade;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    private String productName;

    private long originalAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    protected Order() {
    }

    private Order(String productName, Money originalAmount, Member member) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        this.productName = productName;
        this.originalAmount = Objects.requireNonNull(originalAmount, "주문 원가는 필수입니다.").getValue();
        this.member = Objects.requireNonNull(member, "회원은 필수입니다.");
    }

    public static Order create(String productName, Money originalAmount, Member member) {
        return new Order(productName, originalAmount, member);
    }

    public Money getOriginalAmount() {
        return Money.of(originalAmount);
    }

    public MemberGrade memberGrade() {
        return member.getGrade();
    }
}