package com.polycube.assignment.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.polycube.assignment.common.error.BusinessException;
import com.polycube.assignment.common.error.ErrorCode;
import com.polycube.assignment.common.money.Money;
import com.polycube.assignment.discount.domain.DiscountCalculatorProvider;
import com.polycube.assignment.discount.domain.DiscountSource;
import com.polycube.assignment.discount.domain.DiscountType;
import com.polycube.assignment.discount.domain.FixedDiscountCalculator;
import com.polycube.assignment.discount.domain.GradeDiscountPolicy;
import com.polycube.assignment.discount.domain.NoDiscountCalculator;
import com.polycube.assignment.discount.domain.RateDiscountCalculator;
import com.polycube.assignment.discount.infra.GradeDiscountPolicyRepository;
import com.polycube.assignment.member.domain.Member;
import com.polycube.assignment.member.domain.MemberGrade;
import com.polycube.assignment.order.domain.Order;
import com.polycube.assignment.order.infra.OrderRepository;
import com.polycube.assignment.payment.domain.Payment;
import com.polycube.assignment.payment.domain.PaymentDiscountSnapshot;
import com.polycube.assignment.payment.domain.PaymentMethod;
import com.polycube.assignment.payment.infra.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Instant PAID_AT = Instant.parse("2026-07-21T00:00:00Z");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GradeDiscountPolicyRepository gradeDiscountPolicyRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        DiscountCalculatorProvider calculatorProvider = new DiscountCalculatorProvider(List.of(
                new NoDiscountCalculator(),
                new FixedDiscountCalculator(),
                new RateDiscountCalculator()
        ));

        Clock fixedClock = Clock.fixed(PAID_AT, ZoneOffset.UTC);
        paymentService = new PaymentService(
                paymentRepository,
                orderRepository,
                gradeDiscountPolicyRepository,
                calculatorProvider,
                fixedClock
        );
    }

    @Test
    @DisplayName("결제는 주문과 회원 정보를 결제 시점 값으로 저장한다")
    void completesPaymentWithOrderSnapshot() {
        Order order = Order.create(
                "keyboard",
                Money.of(10_000),
                Member.create(MemberGrade.VIP)
        );
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(gradeDiscountPolicyRepository.findByMemberGradeAndActiveTrue(MemberGrade.VIP))
                .thenReturn(Optional.of(vipPolicy()));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Payment payment = paymentService.pay(1L, PaymentMethod.CARD);

        assertThat(payment.getOrderId()).isEqualTo(1L);
        assertThat(payment.getProductName()).isEqualTo("keyboard");
        assertThat(payment.getOriginalAmount()).isEqualTo(Money.of(10_000));
        assertThat(payment.getMemberGrade()).isEqualTo(MemberGrade.VIP);
        assertThat(payment.getFinalAmount()).isEqualTo(Money.of(9_000));
        assertThat(payment.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getPaidAt()).isEqualTo(PAID_AT);
        assertThat(payment.getDiscountSnapshots()).hasSize(1);
        assertThat(payment.getDiscountSnapshots().get(0).getSource())
                .isEqualTo(DiscountSource.GRADE);
    }

    @Test
    @DisplayName("포인트 결제는 등급 할인 후 5% 할인을 추가 적용한다")
    void appliesPointDiscountAfterGradeDiscount() {
        Order order = Order.create(
                "keyboard",
                Money.of(10_000),
                Member.create(MemberGrade.VIP)
        );
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(false);
        when(gradeDiscountPolicyRepository.findByMemberGradeAndActiveTrue(MemberGrade.VIP))
                .thenReturn(Optional.of(vipPolicy()));
        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.pay(1L, PaymentMethod.POINT);

        List<PaymentDiscountSnapshot> snapshots = payment.getDiscountSnapshots();
        assertThat(snapshots).hasSize(2);
        assertThat(snapshots.get(0).getSource()).isEqualTo(DiscountSource.GRADE);
        assertThat(snapshots.get(0).getFinalAmount()).isEqualTo(Money.of(9_000));
        assertThat(snapshots.get(1).getSource()).isEqualTo(DiscountSource.PAYMENT_METHOD);
        assertThat(snapshots.get(1).getBaseAmount()).isEqualTo(Money.of(9_000));
        assertThat(payment.getFinalAmount()).isEqualTo(Money.of(8_550));
    }

    @Test
    @DisplayName("존재하지 않는 주문은 결제할 수 없다")
    void rejectsUnknownOrder() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.pay(99L, PaymentMethod.POINT))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }

    @Test
    @DisplayName("이미 결제된 주문은 다시 결제할 수 없다")
    void rejectsDuplicatePayment() {
        Order order = Order.create(
                "keyboard",
                Money.of(10_000),
                Member.create(MemberGrade.VIP)
        );
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> paymentService.pay(1L, PaymentMethod.POINT))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode())
                                .isEqualTo(ErrorCode.ORDER_ALREADY_PAID));
    }

    private GradeDiscountPolicy vipPolicy() {
        return GradeDiscountPolicy.create(
                "VIP_FIXED",
                MemberGrade.VIP,
                DiscountType.FIXED,
                new BigDecimal("1000")
        );
    }
}
