package com.polycube.assignment.discount.domain;

import com.polycube.assignment.member.domain.MemberGrade;

public interface DiscountPolicy {

    boolean supports(MemberGrade grade);

    DiscountResult apply(DiscountContext context);
}
