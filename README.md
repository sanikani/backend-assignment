# 결제 시스템 구축 과제

비즈니스 요구사항이 자주 바뀔 수 있다는 전제에서, 할인 규칙의 변경이 결제 흐름과 과거 결제 데이터에 미치는 영향을 줄이는 데 집중했습니다.

`main` 브랜치에서는 기본 요구사항을 단순하고 명확한 코드 기반 정책으로 구현했습니다. `feature` 브랜치에서는 운영 중 할인 정책이 짧은 주기로 변경되고, 정책이 수정되거나 비활성화된 후에도 과거 결제 이력을 보존해야 한다는 추가 요구사항을 반영해 **DB 기반 정책 관리와 결제 시점 할인 스냅샷 구조**로 확장했습니다.

## 1. 구현 범위

| 요구사항 | 구현 내용 |
| --- | --- |
| 회원 | `NORMAL`, `VIP`, `VVIP` 등급 관리 |
| 주문 | 상품명, 주문 원가, 주문 회원 저장 |
| 등급 할인 | 할인 없음, 고정 금액, 비율 할인 계산 지원 |
| 등급 정책 관리 | 정책명, 대상 등급, 할인 유형과 값, 활성 상태를 DB에 저장 |
| 결제 수단 | 신용카드(`CARD`), 포인트(`POINT`) |
| 중복 할인 | 등급 할인 후 포인트 결제 시 5% 추가 할인 |
| 결제 기록 | 주문 정보, 최종 금액, 결제 수단, 결제 시각 저장 |
| 할인 이력 | 적용된 등급과 정책명, 할인 유형과 값, 단계별 금액을 결제 시점 값으로 저장 |
| 이력 보존 | 정책 변경·비활성화 이후에도 과거 결제의 할인 이력 유지 |
| 결제 중복 방지 | 애플리케이션 사전 확인과 DB 유일성 제약 적용 |

## 2. `main`에서 `feature`로의 설계 변경

### 2.1 `main`: 코드 기반 할인 정책

기본 요구사항에서는 회원 등급별 할인 규칙이 명확하고 종류가 적었기 때문에 각 규칙을 코드로 분리했습니다.

```text
MemberGrade
    → GradeDiscountPolicyProvider
        → NormalDiscountPolicy
        → VipFixedDiscountPolicy
        → VvipRateDiscountPolicy
```

이 구조는 다음 장점이 있었습니다.

- 등급별 할인 규칙이 각각의 클래스에 명확하게 드러남
- 결제 서비스가 구체적인 계산 방법을 직접 알지 않음
- 각 등급 정책을 독립적인 단위 테스트로 검증 가능

기본 요구사항을 구현하기에는 충분했지만, 운영 중 정책이 자주 변경된다는 추가 요구사항을 적용하면 한계가 생깁니다.

- 할인 금액이나 할인율을 바꾸려면 코드를 수정하고 다시 배포해야 함
- 정책명과 적용 값이 코드에 묶여 운영 데이터로 관리하기 어려움
- 과거 결제가 어떤 정책과 값으로 계산됐는지 충분히 남지 않음
- 정책 구현체가 늘어날수록 정책 선택과 관리 구조도 함께 커짐

### 2.2 `feature`: DB 기반 등급 할인 정책

`feature`에서는 운영 중 자주 변경되는 대상을 **등급 할인 정책 데이터**로 판단했습니다. 이를 `GradeDiscountPolicy` 엔티티로 만들고 다음 값을 DB에서 관리하도록 변경했습니다.

- 정책명
- 적용 회원 등급
- 할인 유형: `NONE`, `FIXED`, `RATE`
- 할인 값
- 활성 상태

```text
MemberGrade
    → GradeDiscountPolicyRepository
        → 활성 GradeDiscountPolicy 조회
            → DiscountCalculatorProvider
                → NONE / FIXED / RATE 계산기
```

정책 데이터와 계산 알고리즘도 분리했습니다.

- **정책 데이터**는 어떤 등급에 어떤 유형과 값을 적용할지 결정합니다.
- **계산기**는 고정 금액과 비율 같은 계산 방법만 담당합니다.

예를 들어 VIP 할인 금액이 1,000원에서 1,500원으로 변경되는 경우 계산 알고리즘은 동일합니다. 따라서 새로운 클래스를 만들거나 결제 코드를 수정하지 않고 정책 데이터만 변경할 수 있습니다. 반대로 새로운 계산 방식이 추가되는 경우에만 `DiscountCalculator` 구현을 확장하면 됩니다.

정책 생성, 수정, 활성화와 비활성화는 `DiscountPolicyManagementService`가 담당합니다. 결제 서비스는 활성화된 정책만 조회하므로 비활성화된 정책이 신규 결제에 적용되지 않습니다.

`main`에서는 등급별 할인 규칙이 몇 가지로 정해져 있어 각각을 코드로 분리하는 것만으로 충분했습니다. `feature`에서는 할인 정책이 자주 바뀐다는 조건이 추가되어, 변경 대상인 등급 정책을 DB에서 관리하도록 바꿨습니다. 포인트 할인은 현재 5%로 고정된 규칙이므로 같은 구조로 확장하지 않고 단순하게 유지했습니다.

## 3. 결제 시점 할인 스냅샷

### 3.1 정책 참조가 아닌 당시 값 저장

할인 정책을 DB에서 관리하더라도 결제가 정책 엔티티만 참조하면 문제가 생깁니다. 결제 후 정책명이 바뀌거나 할인 값이 수정되면, 현재 정책만으로는 과거 결제에 실제로 적용된 내용을 알 수 없기 때문입니다.

이를 해결하기 위해 할인 계산이 끝날 때 `AppliedDiscount`를 만들고, 결제를 저장할 때 이를 `PaymentDiscountSnapshot`으로 복사합니다.

스냅샷에는 다음 정보를 저장합니다.

| 필드 | 의미 |
| --- | --- |
| `source` | 등급 할인 또는 결제 수단 할인 |
| `target` | `VIP`, `VVIP`, `POINT` 등 적용 대상 |
| `policyName` | 결제 당시 정책명 |
| `discountType` | `NONE`, `FIXED`, `RATE` |
| `discountValue` | 결제 당시 고정 금액 또는 할인율 |
| `baseAmount` | 해당 할인을 적용하기 전 금액 |
| `discountAmount` | 해당 단계에서 할인된 금액 |
| `finalAmount` | 해당 할인을 적용한 후 금액 |

```text
변경 가능한 현재 정책
GradeDiscountPolicy
        │ 계산 시 값 복사
        ▼
AppliedDiscount
        │ 결제 완료 시 영속 값으로 변환
        ▼
PaymentDiscountSnapshot
```

`PaymentDiscountSnapshot`은 `GradeDiscountPolicy`와 외래 키나 JPA 연관관계를 갖지 않습니다. 정책의 현재 상태가 아니라 결제 당시 확정된 사실을 저장하는 것이 목적이기 때문입니다. 따라서 정책이 수정되거나 비활성화되어도 기존 결제 이력은 변경되지 않습니다.

### 3.2 `@ElementCollection`을 선택한 이유

할인 스냅샷은 독립적으로 생성하거나 수정하는 데이터가 아니라 결제에 포함된 값입니다. 별도의 식별자와 생명주기를 갖는 엔티티보다 `Payment`가 소유하는 값 객체가 책임에 더 잘 맞는다고 판단했습니다.

```text
payments
    └── payment_discount_snapshots
          - payment_id
          - discount_sequence
          - 적용 당시 할인 정보
```

- `Payment`가 저장되거나 삭제될 때 같은 생명주기를 따름
- 별도 ID, Repository와 서비스가 필요하지 않음
- `discount_sequence`로 적용 순서를 보존
- 정책 테이블과 분리되어 과거 값의 불변성을 유지

스냅샷을 별도 이력 엔티티로 만들면 독립적인 조회와 수정 책임까지 생기지만 현재 요구사항에는 필요하지 않습니다. 결제의 구성 값이라는 의미를 유지하면서 필요한 이력만 저장하기 위해 `@ElementCollection`을 선택했습니다.

## 4. 중복 할인과 적용 순서

포인트 결제는 등급 할인이 적용된 최종 금액에서 추가로 5%를 할인합니다. 할인 결과는 순서에 따라 달라지므로 `PaymentService`에서 다음 흐름을 명시적으로 보장합니다.

```text
주문 원가 10,000원
    → VIP 등급 할인 1,000원
    → 9,000원
    → 포인트 결제 할인 5%인 450원
    → 최종 결제 금액 8,550원
```

저장되는 스냅샷도 같은 순서를 가집니다.

| 순서 | 할인 출처 | 기준 금액 | 할인 금액 | 결과 금액 |
| ---: | --- | ---: | ---: | ---: |
| 0 | 등급 할인 | 10,000원 | 1,000원 | 9,000원 |
| 1 | 포인트 결제 할인 | 9,000원 | 450원 | 8,550원 |

등급 할인은 운영상 자주 변경되는 정책으로 명시되어 있어 DB로 관리했지만, 포인트 결제 5% 할인은 현재 하나의 고정 요구사항입니다. 따라서 결제 수단 할인까지 별도 테이블이나 범용 정책 엔진으로 만들지 않고 `PointPaymentDiscountPolicy`로 단순하게 구현했습니다.

추후 결제 수단별 정책이 여러 개로 늘어나거나 중복 가능 여부와 적용 순서까지 자주 변경된다면 다음 정보를 별도 정책으로 관리할 수 있습니다.

- 적용 가능한 결제 수단
- 중복 할인 허용 여부
- 할인 우선순위
- 활성 기간과 상태

현재 요구사항에는 이 복잡도가 필요하지 않으므로 확장 가능성은 남기되 미리 구현하지 않았습니다.

## 5. 결제 처리 흐름

```text
PaymentService.pay(orderId, paymentMethod)
    1. 주문 조회
       └─ 없으면 ORDER_NOT_FOUND
    2. 기존 결제 여부 확인
       └─ 이미 결제됐으면 ORDER_ALREADY_PAID
    3. 회원 등급에 해당하는 활성 GradeDiscountPolicy 조회
       └─ 없으면 GRADE_DISCOUNT_POLICY_NOT_FOUND
    4. 할인 유형에 맞는 DiscountCalculator로 등급 할인 계산
    5. 등급 정책 정보와 계산 결과를 AppliedDiscount로 생성
    6. POINT 결제라면 등급 할인 후 금액에 5% 추가 할인
    7. 적용 순서대로 PaymentDiscountSnapshot 생성
    8. 주문·결제 정보와 할인 이력을 Payment로 저장
```

## 6. 데이터 저장 구조

```text
grade_discount_policies
    - id
    - name
    - member_grade
    - discount_type
    - discount_value
    - active

payments
    - id
    - order_id
    - product_name
    - original_amount
    - discount_amount
    - final_amount
    - member_grade
    - payment_method
    - paid_at

payment_discount_snapshots
    - payment_id
    - discount_sequence
    - source
    - target
    - policy_name
    - discount_type
    - discount_value
    - base_amount
    - discount_amount
    - final_amount
```

정책 테이블은 현재 적용할 규칙을 관리하고, 결제와 할인 스냅샷 테이블은 이미 완료된 결제 사실을 보존합니다. 두 목적을 분리해 운영 정책 변경이 과거 데이터에 전파되지 않도록 했습니다.

## 7. 테스트 전략

`feature` 브랜치에 명시된 추가 테스트 요구사항을 기준으로 검증했습니다.

| 요구사항 | 검증 내용 |
| --- | --- |
| 등급 할인과 결제 수단 할인의 우선순위 및 최종 금액 | `PaymentServiceTest`에서 VIP 10,000원 주문에 등급 할인을 먼저 적용하고, 남은 9,000원에 포인트 5% 할인을 적용해 최종 금액이 8,550원인지 검증 |
| 정책 수정·삭제 후 과거 결제 데이터와 이력 보존 | `PaymentDiscountHistoryTest`에서 정책 변경 전후의 결제를 저장하고 정책을 비활성화한 뒤, 각 결제의 정책명과 할인 금액이 결제 당시 값으로 유지되는지 H2에서 재조회하여 검증 |
