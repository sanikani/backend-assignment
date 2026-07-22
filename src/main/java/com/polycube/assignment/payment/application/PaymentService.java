package com.polycube.assignment.payment.application;

import com.polycube.assignment.common.error.BusinessException;
import com.polycube.assignment.common.error.ErrorCode;
import com.polycube.assignment.discount.domain.DiscountContext;
import com.polycube.assignment.discount.domain.DiscountResult;
import com.polycube.assignment.discount.domain.GradeDiscountPolicyProvider;
import com.polycube.assignment.order.domain.Order;
import com.polycube.assignment.order.infra.OrderRepository;
import com.polycube.assignment.payment.domain.Payment;
import com.polycube.assignment.payment.domain.PaymentMethod;
import com.polycube.assignment.payment.infra.PaymentRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final GradeDiscountPolicyProvider policyProvider;
    private final Clock clock;

    @Transactional
    public Payment pay(Long orderId, PaymentMethod paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (paymentRepository.existsByOrderId(orderId)) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }

        DiscountContext context = new DiscountContext(order.getOriginalAmount(), order.memberGrade());
        DiscountResult result = policyProvider.get(order.memberGrade()).apply(context);
        Payment payment = Payment.complete(orderId, order, result, paymentMethod, clock.instant());
        return paymentRepository.save(payment);
    }
}
