package com.polycube.assignment.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private MemberGrade grade;

    private Member(MemberGrade grade) {
        Objects.requireNonNull(grade, "회원 등급은 필수입니다");
        this.grade = grade;
    }

    public static Member create(MemberGrade grade) {
        return new Member(grade);
    }
}
