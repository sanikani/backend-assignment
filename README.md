# 결제 시스템 구축 과제

비즈니스 정책이 자주 변경될 수 있다는 요구사항을 고려해 할인 계산을 결제 유스케이스에서 분리하고, 금액과 결제 이력을 안전하게 다루는 데 집중했습니다.

## 1. 구현 범위

| 요구사항 | 구현 내용 |
| --- | --- |
| 회원 | `NORMAL`, `VIP`, `VVIP` 등급 관리 |
| 주문 | 상품명, 주문 원가, 주문 회원 저장 |
| NORMAL 할인 | 할인 없음 |
| VIP 할인 | 1,000원 고정 할인, 주문 원가를 초과하지 않음 |
| VVIP 할인 | 주문 원가의 10% 할인, 1원 미만 버림 |
| 결제 수단 | 신용카드(`CARD`), 포인트(`POINT`) |
| 결제 기록 | 주문 식별자, 상품명, 원가, 할인 금액, 최종 금액, 회원 등급, 결제 수단, 결제 일시 저장 |
| 중복 결제 | 애플리케이션 사전 확인과 DB 유일성 제약으로 방지 |
| 테스트 | 할인 정책 단위 테스트, 결제 서비스 단위 테스트, H2 영속성 테스트 |

## 2. 기술 스택

- Java 17
- Spring Boot 3.5.16
- Spring Data JPA
- H2 Database
- Lombok
- JUnit 5, AssertJ, Mockito
- Gradle Wrapper

## 3. 패키지 구조

```text
com.polycube.assignment
├── common
│   ├── config       # Clock 등 공통 설정
│   ├── error        # 비즈니스 예외와 오류 코드
│   └── money        # Money 값 객체
├── member
│   ├── domain       # Member, MemberGrade
│   └── infra        # MemberRepository
├── order
│   ├── domain       # Order
│   └── infra        # OrderRepository
├── discount
│   └── domain       # 할인 정책, 계산 입력과 결과, 정책 Provider
└── payment
    ├── application  # PaymentService
    ├── domain       # Payment, PaymentMethod
    └── infra        # PaymentRepository
```

## 4. 핵심 설계

### 4.1 결제 서비스와 분리된 할인 정책

회원 등급별 할인 로직을 `PaymentService`의 조건문에 모으면 정책이 추가되거나 변경될 때 결제 유스케이스도 함께 수정해야 합니다. 이를 피하기 위해 할인 규칙을 `DiscountPolicy` 구현체로 분리했습니다.

```java
public interface DiscountPolicy {
    boolean supports(MemberGrade grade);
    DiscountResult apply(DiscountContext context);
}
```

- `NormalDiscountPolicy`: 할인 없음
- `VipFixedDiscountPolicy`: 1,000원 고정 할인
- `VvipRateDiscountPolicy`: 주문 원가의 10% 할인

`PaymentService`는 구체적인 정책을 알지 않고 `GradeDiscountPolicyProvider`에 회원 등급에 맞는 정책을 요청합니다. 따라서 새로운 할인 규칙은 `DiscountPolicy` 구현체를 추가하는 방식으로 확장할 수 있으며, 결제 흐름은 할인 계산 방법과 분리됩니다.

Provider는 정책 조회 시 해당 등급을 지원하는 정책이 정확히 하나인지 검증합니다. 지원 정책이 없거나 둘 이상이면 `IllegalStateException`을 발생시켜 할인 계산과 결제 저장이 잘못된 정책 구성으로 진행되지 않도록 합니다.

### 4.2 `Money` 값 객체를 통한 금액 안정성

금액을 원시 타입으로만 다루면 음수 금액이나 잘못된 할인율을 여러 위치에서 반복해서 방어해야 합니다. 금액 생성과 계산 규칙을 `Money`에 집중시켜 동일한 규칙이 일관되게 적용되도록 했습니다.

- 음수 금액 생성 방지
- 차감 결과가 0원 미만으로 내려가지 않도록 보정
- 할인율을 0 이상 1 이하로 제한
- 비율 계산에 `BigDecimal`을 사용해 부동소수점 오차 방지
- 원화 단위에 맞춰 1원 미만을 `RoundingMode.DOWN`으로 처리
- 값 기반 `equals`와 `hashCode` 제공

엔티티의 영속성 필드는 원화 금액을 `long`으로 저장해 JPA 매핑을 단순하게 유지했습니다. 공개 접근자에서는 저장된 값을 `Money`로 복원해 금액 관련 도메인 규칙과 값 동등성을 유지합니다.

### 4.3 유효한 상태로 생성되는 도메인 객체

유효하지 않은 객체를 생성한 뒤 서비스에서 검사하는 대신, 각 객체가 생성 시점에 자신의 필수 값을 검증하도록 했습니다.

- `Member`: 회원 등급 필수
- `Order`: 공백이 아닌 상품명, 주문 원가, 회원 필수
- `DiscountContext`: 주문 원가와 회원 등급 필수
- `DiscountResult`: 주문 원가와 할인 금액 필수
- `Payment`: 주문 식별자, 주문, 할인 결과, 결제 수단, 결제 일시 필수

JPA 엔티티인 `Member`, `Order`, `Payment`는 기본 생성자의 접근 수준을 `protected`로 제한하고 정적 팩토리를 통해 생성합니다. 이를 통해 영속성 프레임워크의 요구사항을 충족하면서 외부에서 불완전한 엔티티가 만들어질 가능성을 줄였습니다.

### 4.4 결제 시점 스냅샷

결제 데이터는 현재 주문이나 회원의 상태가 아니라 결제가 완료된 시점의 사실을 나타내야 합니다. 회원 등급이나 주문 정보가 나중에 바뀌더라도 저장된 결제 결과가 함께 변하지 않도록 다음 값을 `Payment`에 복사해 저장합니다.

- 주문 식별자와 상품명
- 주문 원가
- 할인 금액과 최종 결제 금액
- 결제 당시 회원 등급
- 결제 수단
- 결제 일시

`Payment`가 `Order`나 `Member`를 연관관계로 탐색해 결제 결과를 다시 계산하지 않기 때문에, 결제 당시의 금액과 등급을 독립적으로 조회할 수 있습니다.

현재 `main`에서는 금액과 등급 스냅샷까지 보존합니다. 적용 정책명, 할인율, 정책 버전은 아직 저장하지 않으며 `feature`에서 정책 자체의 이력까지 확장할 예정입니다.

### 4.5 애플리케이션과 DB의 이중 중복 결제 방지

`PaymentService`는 결제를 저장하기 전에 `existsByOrderId(orderId)`로 이미 결제된 주문인지 확인합니다. 일반적인 중복 요청은 `ORDER_ALREADY_PAID` 비즈니스 예외로 빠르게 응답할 수 있습니다.

동시에 `payments.order_id`에는 `uk_payments_order_id` 유일성 제약을 적용했습니다. 조회와 저장 사이에 다른 요청이 끼어들 수 있는 동시성 상황에서도 DB가 동일 주문의 결제 레코드를 둘 이상 저장하지 않도록 하는 최종 방어선입니다.

### 4.6 테스트 가능한 결제 시간

`Payment`에서 `Instant.now()`를 직접 호출하면 테스트할 때 결제 시각을 고정하기 어렵습니다. 시스템 시간을 외부 의존성으로 보고 `Clock`을 `PaymentService`에 주입했습니다.

```text
운영: Clock.systemUTC()
테스트: Clock.fixed(...)
```

운영 환경은 UTC 현재 시각을 사용하고, 테스트에서는 고정된 시각을 주입해 결제 일시까지 결정적으로 검증합니다. `Payment`는 현재 시간을 직접 생성하지 않고 서비스가 전달한 `paidAt`을 결제 사실의 일부로 저장합니다.

### 4.7 트랜잭션 경계와 비즈니스 예외

결제 유스케이스의 트랜잭션 경계는 `PaymentService.pay()`입니다. 주문 조회, 중복 결제 확인, 할인 계산, 결제 생성과 저장을 하나의 트랜잭션 안에서 수행해 결제 흐름의 원자성을 유지합니다.

예상 가능한 실패는 `BusinessException`과 `ErrorCode`로 표현합니다.

- 존재하지 않는 주문: `ORDER_NOT_FOUND`
- 이미 결제된 주문: `ORDER_ALREADY_PAID`

이를 통해 애플리케이션 계층이 저장소의 세부 예외가 아니라 결제 유스케이스의 의미가 드러나는 오류를 전달합니다. 단, 동시에 들어온 요청이 DB 유일성 제약에서 충돌하는 경우의 영속성 예외를 별도 `BusinessException`으로 변환하는 처리는 현재 범위에 포함하지 않았습니다.

## 5. 결제 처리 흐름

```text
PaymentService.pay(orderId, paymentMethod)
    1. 주문 조회
       └─ 없으면 ORDER_NOT_FOUND
    2. 기존 결제 여부 확인
       └─ 이미 결제됐으면 ORDER_ALREADY_PAID
    3. 주문 원가와 회원 등급으로 DiscountContext 생성
    4. GradeDiscountPolicyProvider에서 등급 정책 선택
    5. 할인 금액과 최종 금액 계산
    6. 결제 시각을 포함한 Payment 스냅샷 생성
    7. Payment 저장
```

## 6. 테스트 전략

| 테스트 | 검증 목적 |
| --- | --- |
| `NormalDiscountPolicyTest` | NORMAL 회원에게 할인이 적용되지 않는지 검증 |
| `VipFixedDiscountPolicyTest` | VIP 1,000원 할인과 주문 원가 초과 방지 검증 |
| `VvipRateDiscountPolicyTest` | VVIP 10% 할인과 1원 미만 절사 검증 |
| `GradeDiscountPolicyProviderTest` | 모든 등급에 정책이 정확히 하나 존재하는지 검증 |
| `MoneyTest` | 금액 생성, 차감, 비율 계산과 값 동등성 검증 |
| `MemberTest`, `OrderTest` | 엔티티 생성 규칙과 필수 값 검증 |
| `PaymentServiceTest` | 저장소를 Mock 처리해 결제 성공, 주문 미존재, 중복 결제 흐름 검증 |
| `PaymentPersistenceTest` | 실제 H2 저장 후 영속성 컨텍스트를 초기화하고 동일한 결제 스냅샷이 조회되는지 검증 |