package com.polycube.assignment.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.discount.application.DiscountPolicyManagementService;
import com.polycube.assignment.discount.domain.DiscountType;
import com.polycube.assignment.discount.domain.GradeDiscountPolicy;
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
@Import(PaymentDiscountHistoryTest.FixedClockConfiguration.class)
class PaymentDiscountHistoryTest {

    private static final Instant PAID_AT = Instant.parse("2026-07-21T00:00:00Z");

    @Autowired
    private DiscountPolicyManagementService policyManagementService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private GradeDiscountPolicyRepository gradeDiscountPolicyRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("정책 변경과 비활성화 후에도 결제 당시 할인 이력을 보존한다")
    void preservesDiscountHistoryAfterPolicyUpdateAndDeactivation() {
        GradeDiscountPolicy policy = policyManagementService.create(
                "VIP_FIXED_V1",
                MemberGrade.VIP,
                DiscountType.FIXED,
                new BigDecimal("1000")
        );
        Member member = memberRepository.save(Member.create(MemberGrade.VIP));
        Order firstOrder = orderRepository.save(
                Order.create("keyboard", Money.of(10_000), member)
        );
        Payment firstPayment = paymentService.pay(firstOrder.getId(), PaymentMethod.CARD);

        policyManagementService.update(
                policy.getId(),
                "VIP_FIXED_V2",
                DiscountType.FIXED,
                new BigDecimal("1500")
        );
        Order secondOrder = orderRepository.save(
                Order.create("mouse", Money.of(10_000), member)
        );
        Payment secondPayment = paymentService.pay(secondOrder.getId(), PaymentMethod.CARD);
        policyManagementService.deactivate(policy.getId());

        paymentRepository.flush();
        entityManager.clear();

        Payment foundFirstPayment = paymentRepository.findById(firstPayment.getId()).orElseThrow();
        Payment foundSecondPayment = paymentRepository.findById(secondPayment.getId()).orElseThrow();
        PaymentDiscountSnapshot firstSnapshot = foundFirstPayment.getDiscountSnapshots().get(0);
        PaymentDiscountSnapshot secondSnapshot = foundSecondPayment.getDiscountSnapshots().get(0);

        assertThat(gradeDiscountPolicyRepository.findByMemberGradeAndActiveTrue(MemberGrade.VIP))
                .isEmpty();
        assertThat(firstSnapshot.getPolicyName()).isEqualTo("VIP_FIXED_V1");
        assertThat(firstSnapshot.getTarget()).isEqualTo("VIP");
        assertThat(firstSnapshot.getDiscountAmount()).isEqualTo(Money.of(1_000));
        assertThat(foundFirstPayment.getFinalAmount()).isEqualTo(Money.of(9_000));

        assertThat(secondSnapshot.getPolicyName()).isEqualTo("VIP_FIXED_V2");
        assertThat(secondSnapshot.getTarget()).isEqualTo("VIP");
        assertThat(secondSnapshot.getDiscountAmount()).isEqualTo(Money.of(1_500));
        assertThat(foundSecondPayment.getFinalAmount()).isEqualTo(Money.of(8_500));
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
