package com.polycube.assignment.payment.application;

import com.polycube.assignment.common.error.BusinessException;
import com.polycube.assignment.common.error.ErrorCode;
import com.polycube.assignment.discount.domain.AppliedDiscount;
import com.polycube.assignment.discount.domain.DiscountCalculatorProvider;
import com.polycube.assignment.discount.domain.DiscountResult;
import com.polycube.assignment.discount.domain.GradeDiscountPolicy;
import com.polycube.assignment.discount.domain.PointPaymentDiscountPolicy;
import com.polycube.assignment.discount.infra.GradeDiscountPolicyRepository;
import com.polycube.assignment.order.domain.Order;
import com.polycube.assignment.order.infra.OrderRepository;
import com.polycube.assignment.payment.domain.Payment;
import com.polycube.assignment.payment.domain.PaymentMethod;
import com.polycube.assignment.payment.infra.PaymentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final GradeDiscountPolicyRepository gradeDiscountPolicyRepository;
    private final DiscountCalculatorProvider calculatorProvider;
    private final Clock clock;
    private final PointPaymentDiscountPolicy pointPaymentDiscountPolicy =
            new PointPaymentDiscountPolicy();

    @Transactional
    public Payment pay(Long orderId, PaymentMethod paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        GradeDiscountPolicy gradePolicy = gradeDiscountPolicyRepository
                .findByMemberGradeAndActiveTrue(order.memberGrade())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.GRADE_DISCOUNT_POLICY_NOT_FOUND
                ));
        DiscountResult gradeResult = calculatorProvider.get(gradePolicy.getDiscountType())
                .calculate(order.getOriginalAmount(), gradePolicy.getDiscountValue());

        AppliedDiscount gradeDiscount = AppliedDiscount.forGrade(gradePolicy, gradeResult);
        List<AppliedDiscount> appliedDiscounts = new ArrayList<>();
        appliedDiscounts.add(gradeDiscount);
        if (paymentMethod == PaymentMethod.POINT) {
            appliedDiscounts.add(pointPaymentDiscountPolicy.apply(gradeDiscount.getFinalAmount()));
        }

        Payment payment = Payment.complete(
                orderId,
                order,
                appliedDiscounts,
                paymentMethod,
                clock.instant()
        );
        return paymentRepository.save(payment);
    }
}
