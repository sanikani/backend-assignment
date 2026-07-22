package com.polycube.assignment.payment.domain;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.discount.domain.AppliedDiscount;
import com.polycube.assignment.discount.domain.DiscountResult;
import com.polycube.assignment.member.domain.MemberGrade;
import com.polycube.assignment.order.domain.Order;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "payments",
        uniqueConstraints =
        @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(nullable = false)
    @Getter
    private long orderId;

    @Column(nullable = false)
    @Getter
    private String productName;

    @Column(nullable = false)
    private long originalAmount;

    @Column(nullable = false)
    private long discountAmount;

    @Column(nullable = false)
    private long finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    private MemberGrade memberGrade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    @Getter
    private Instant paidAt;

    @ElementCollection
    @CollectionTable(
            name = "payment_discount_snapshots",
            joinColumns = @JoinColumn(name = "payment_id")
    )
    @OrderColumn(name = "discount_sequence")
    private List<PaymentDiscountSnapshot> discountSnapshots = new ArrayList<>();

    private Payment(
            Long orderId,
            Order order,
            DiscountResult discountResult,
            PaymentMethod paymentMethod,
            Instant paidAt
    ) {
        this.orderId = Objects.requireNonNull(orderId, "주문 식별자는 필수입니다.");
        this.productName = order.getProductName();
        this.originalAmount = discountResult.getOriginalAmount().getValue();
        this.discountAmount = discountResult.getDiscountAmount().getValue();
        this.finalAmount = discountResult.getFinalAmount().getValue();
        this.memberGrade = order.memberGrade();
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "결제 수단은 필수입니다.");
        this.paidAt = Objects.requireNonNull(paidAt, "결제 일시는 필수입니다.");
    }

    public static Payment complete(
            Long orderId,
            Order order,
            DiscountResult discountResult,
            PaymentMethod paymentMethod,
            Instant paidAt
    ) {
        Objects.requireNonNull(order, "주문은 필수입니다.");
        Objects.requireNonNull(discountResult, "할인 결과는 필수입니다.");
        return new Payment(orderId, order, discountResult, paymentMethod, paidAt);
    }

    public static Payment complete(
            Long orderId,
            Order order,
            List<AppliedDiscount> appliedDiscounts,
            PaymentMethod paymentMethod,
            Instant paidAt
    ) {
        Objects.requireNonNull(order, "주문은 필수입니다.");
        Objects.requireNonNull(appliedDiscounts, "적용된 할인 목록은 필수입니다.");

        Money originalAmount = order.getOriginalAmount();
        Money finalAmount = originalAmount;
        if (!appliedDiscounts.isEmpty()) {
            finalAmount = appliedDiscounts.get(appliedDiscounts.size() - 1).getFinalAmount();
        }

        Payment payment = new Payment(
                orderId,
                order,
                DiscountResult.of(
                        originalAmount,
                        originalAmount.subtract(finalAmount)
                ),
                paymentMethod,
                paidAt
        );

        appliedDiscounts.stream()
                .map(PaymentDiscountSnapshot::from)
                .forEach(payment.discountSnapshots::add);
        return payment;
    }

    public List<PaymentDiscountSnapshot> getDiscountSnapshots() {
        return List.copyOf(discountSnapshots);
    }

    public Money getOriginalAmount() {
        return Money.of(originalAmount);
    }

    public Money getDiscountAmount() {
        return Money.of(discountAmount);
    }

    public Money getFinalAmount() {
        return Money.of(finalAmount);
    }
}
