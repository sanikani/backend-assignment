package com.polycube.assignment.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("회원은 등급을 가지고 생성된다")
    void createsMemberWithGrade() {
        Member member = Member.create(MemberGrade.VIP);

        assertThat(member.getGrade()).isEqualTo(MemberGrade.VIP);
    }

    @Test
    @DisplayName("회원 등급은 필수다")
    void rejectsMissingGrade() {
        assertThatThrownBy(() -> Member.create(null))
                .isInstanceOf(NullPointerException.class);
    }
}
