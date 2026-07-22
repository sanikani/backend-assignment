package com.polycube.assignment.discount.infra;

import com.polycube.assignment.discount.domain.GradeDiscountPolicy;
import com.polycube.assignment.member.domain.MemberGrade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeDiscountPolicyRepository
        extends JpaRepository<GradeDiscountPolicy, Long> {

    Optional<GradeDiscountPolicy> findByMemberGrade(MemberGrade memberGrade);

    Optional<GradeDiscountPolicy> findByMemberGradeAndActiveTrue(MemberGrade memberGrade);

    boolean existsByMemberGrade(MemberGrade memberGrade);
}
