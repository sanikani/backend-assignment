package com.polycube.assignment.discount.application;

import com.polycube.assignment.common.error.BusinessException;
import com.polycube.assignment.common.error.ErrorCode;
import com.polycube.assignment.discount.domain.DiscountType;
import com.polycube.assignment.discount.domain.GradeDiscountPolicy;
import com.polycube.assignment.discount.infra.GradeDiscountPolicyRepository;
import com.polycube.assignment.member.domain.MemberGrade;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiscountPolicyManagementService {

    private final GradeDiscountPolicyRepository repository;

    @Transactional
    public GradeDiscountPolicy create(
            String name,
            MemberGrade memberGrade,
            DiscountType discountType,
            BigDecimal discountValue
    ) {
        if (repository.existsByMemberGrade(memberGrade)) {
            throw new BusinessException(ErrorCode.GRADE_DISCOUNT_POLICY_ALREADY_EXISTS);
        }

        return repository.save(GradeDiscountPolicy.create(
                name,
                memberGrade,
                discountType,
                discountValue
        ));
    }

    @Transactional
    public GradeDiscountPolicy update(
            Long policyId,
            String name,
            DiscountType discountType,
            BigDecimal discountValue
    ) {
        GradeDiscountPolicy policy = getPolicy(policyId);
        policy.update(name, discountType, discountValue);
        return policy;
    }

    @Transactional
    public void activate(Long policyId) {
        getPolicy(policyId).activate();
    }

    @Transactional
    public void deactivate(Long policyId) {
        getPolicy(policyId).deactivate();
    }

    private GradeDiscountPolicy getPolicy(Long policyId) {
        return repository.findById(policyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GRADE_DISCOUNT_POLICY_NOT_FOUND));
    }
}
