package com.polycube.assignment.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.member.domain.Member;
import com.polycube.assignment.member.domain.MemberGrade;
import com.polycube.assignment.member.infra.MemberRepository;
import com.polycube.assignment.order.domain.Order;
import com.polycube.assignment.order.infra.OrderRepository;
import com.polycube.assignment.payment.domain.Payment;
import com.polycube.assignment.payment.domain.PaymentMethod;
import com.polycube.assignment.payment.infra.PaymentRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
    private PaymentService paymentService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("완료된 결제를 H2에 저장하고 동일한 스냅샷으로 다시 조회한다")
    void persistsCompletedPayment() {
        Member member = memberRepository.save(Member.create(MemberGrade.VIP));
        Order order = orderRepository.save(Order.create("keyboard", Money.of(10_000), member));

        Payment saved = paymentService.pay(order.getId(), PaymentMethod.POINT);
        paymentRepository.flush();
        entityManager.clear();

        Payment found = paymentRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getOrderId()).isEqualTo(order.getId());
        assertThat(found.getProductName()).isEqualTo("keyboard");
        assertThat(found.getOriginalAmount()).isEqualTo(Money.of(10_000));
        assertThat(found.getDiscountAmount()).isEqualTo(Money.of(1_000));
        assertThat(found.getFinalAmount()).isEqualTo(Money.of(9_000));
        assertThat(found.getMemberGrade()).isEqualTo(MemberGrade.VIP);
        assertThat(found.getPaymentMethod()).isEqualTo(PaymentMethod.POINT);
        assertThat(found.getPaidAt()).isEqualTo(PAID_AT);
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
