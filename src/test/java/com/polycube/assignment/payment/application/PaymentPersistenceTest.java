package com.polycube.assignment.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.discount.domain.AppliedDiscount;
import com.polycube.assignment.discount.domain.DiscountResult;
import com.polycube.assignment.discount.domain.DiscountSource;
import com.polycube.assignment.discount.domain.DiscountType;
import com.polycube.assignment.discount.domain.GradeDiscountPolicy;
import com.polycube.assignment.discount.domain.PointPaymentDiscountPolicy;
import com.polycube.assignment.discount.infra.GradeDiscountPolicyRepository;
import com.polycube.assignment.member.domain.Member;
import com.polycube.assignment.member.domain.MemberGrade;
import com.polycube.assignment.member.infra.MemberRepository;
import com.polycube.assignment.order.domain.Order;
import com.polycube.assignment.order.infra.OrderRepository;
import com.polycube.assignment.payment.domain.Payment;
import com.polycube.assignment.payment.domain.PaymentDiscountSnapshot;
import com.polycube.assignment.payment.domain.PaymentMethod;
import com.polycube.assignment.payment.infra.PaymentRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@Import(PaymentPersistenceTest.FixedClockConfiguration.class)
class PaymentPersistenceTest {

    private static final Instant PAID_AT = Instant.parse("2026-07-21T00:00:00Z");

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private GradeDiscountPolicyRepository gradeDiscountPolicyRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("완료된 결제를 H2에 저장하고 동일한 스냅샷으로 다시 조회한다")
    void persistsCompletedPayment() {
        Member member = memberRepository.save(Member.create(MemberGrade.VIP));
        Order order = orderRepository.save(Order.create("keyboard", Money.of(10_000), member));
        gradeDiscountPolicyRepository.save(GradeDiscountPolicy.create(
                "VIP_FIXED",
                MemberGrade.VIP,
                DiscountType.FIXED,
                new BigDecimal("1000")
        ));

        Payment saved = paymentService.pay(order.getId(), PaymentMethod.POINT);
        paymentRepository.flush();
        entityManager.clear();

        Payment found = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getOrderId()).isEqualTo(order.getId());
        assertThat(found.getProductName()).isEqualTo("keyboard");
        assertThat(found.getOriginalAmount()).isEqualTo(Money.of(10_000));
        assertThat(found.getDiscountAmount()).isEqualTo(Money.of(1_450));
        assertThat(found.getFinalAmount()).isEqualTo(Money.of(8_550));
        assertThat(found.getMemberGrade()).isEqualTo(MemberGrade.VIP);
        assertThat(found.getPaymentMethod()).isEqualTo(PaymentMethod.POINT);
        assertThat(found.getPaidAt()).isEqualTo(PAID_AT);

        List<PaymentDiscountSnapshot> snapshots = found.getDiscountSnapshots();
        assertThat(snapshots).hasSize(2);
        assertThat(snapshots.get(0).getSource()).isEqualTo(DiscountSource.GRADE);
        assertThat(snapshots.get(1).getSource()).isEqualTo(DiscountSource.PAYMENT_METHOD);
    }

    @Test
    @DisplayName("적용된 할인 이력을 순서대로 저장하고 다시 조회한다")
    void persistsAppliedDiscountsInOrder() {
        Member member = memberRepository.save(Member.create(MemberGrade.VIP));
        Order order = orderRepository.save(Order.create("keyboard", Money.of(10_000), member));
        GradeDiscountPolicy gradePolicy = GradeDiscountPolicy.create(
                "VIP_FIXED",
                MemberGrade.VIP,
                DiscountType.FIXED,
                new BigDecimal("1000")
        );
        AppliedDiscount gradeDiscount = AppliedDiscount.forGrade(
                gradePolicy,
                DiscountResult.of(Money.of(10_000), Money.of(1_000))
        );
        AppliedDiscount pointDiscount = new PointPaymentDiscountPolicy()
                .apply(gradeDiscount.getFinalAmount());

        Payment saved = paymentRepository.saveAndFlush(Payment.complete(
                order.getId(),
                order,
                List.of(gradeDiscount, pointDiscount),
                PaymentMethod.POINT,
                PAID_AT
        ));
        entityManager.clear();

        Payment found = paymentRepository.findById(saved.getId()).orElseThrow();

        List<PaymentDiscountSnapshot> snapshots = found.getDiscountSnapshots();
        assertThat(snapshots).hasSize(2);
        PaymentDiscountSnapshot gradeSnapshot = snapshots.get(0);
        PaymentDiscountSnapshot pointSnapshot = snapshots.get(1);

        assertThat(gradeSnapshot.getSource()).isEqualTo(DiscountSource.GRADE);
        assertThat(gradeSnapshot.getTarget()).isEqualTo("VIP");
        assertThat(gradeSnapshot.getPolicyName()).isEqualTo("VIP_FIXED");
        assertThat(gradeSnapshot.getDiscountType()).isEqualTo(DiscountType.FIXED);
        assertThat(gradeSnapshot.getDiscountValue()).isEqualByComparingTo("1000");
        assertThat(gradeSnapshot.getBaseAmount()).isEqualTo(Money.of(10_000));
        assertThat(gradeSnapshot.getDiscountAmount()).isEqualTo(Money.of(1_000));
        assertThat(gradeSnapshot.getFinalAmount()).isEqualTo(Money.of(9_000));

        assertThat(pointSnapshot.getSource()).isEqualTo(DiscountSource.PAYMENT_METHOD);
        assertThat(pointSnapshot.getTarget()).isEqualTo("POINT");
        assertThat(pointSnapshot.getPolicyName()).isEqualTo("POINT_RATE_5_PERCENT");
        assertThat(pointSnapshot.getDiscountType()).isEqualTo(DiscountType.RATE);
        assertThat(pointSnapshot.getDiscountValue()).isEqualByComparingTo("0.05");
        assertThat(pointSnapshot.getBaseAmount()).isEqualTo(Money.of(9_000));
        assertThat(pointSnapshot.getDiscountAmount()).isEqualTo(Money.of(450));
        assertThat(pointSnapshot.getFinalAmount()).isEqualTo(Money.of(8_550));

        assertThat(found.getDiscountAmount()).isEqualTo(Money.of(1_450));
        assertThat(found.getFinalAmount()).isEqualTo(Money.of(8_550));
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(PAID_AT, ZoneOffset.UTC);
        }
    }
}
