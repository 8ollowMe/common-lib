# common-lib

- Java 21 / Spring Boot 3.5.1
- GitHub Packages 배포

---

## 팀원 사용 가이드

### 1. GitHub Personal Access Token 발급

GitHub Packages에서 패키지를 내려받으려면 **인증 토큰**이 필요합니다.

1. GitHub → **Settings** → **Developer settings** → **Personal access tokens** → **Tokens (classic)**
2. **Generate new token (classic)** 클릭
3. 권한에서 **`read:packages`** 체크 (다운로드만 할 경우)
4. 토큰 생성 후 복사 (`ghp_xxxx...`)

---

### 2. Gradle 인증 설정

발급받은 토큰을 **로컬 Gradle 설정 파일**에 저장합니다.
> ⚠️ 프로젝트 내부 파일에 절대 넣지 마세요. 깃에 올라갑니다.

```
~/.gradle/gradle.properties
```

파일을 열어 아래 내용을 추가합니다:

```properties
gpr.user=본인_깃헙_아이디
gpr.key=ghp_xxxxxxxxxxxxxxxxxxxx
```

---

### 3. 프로젝트 build.gradle 설정

#### Gradle Groovy DSL

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/8ollowMe/common-lib")
        credentials {
            username = findProperty("gpr.user")
            password = findProperty("gpr.key")
        }
    }
}

dependencies {
    implementation "com.followMe:common-lib:1.0.0"
}
```

---

## 기능 사용법

### BaseTime / BaseAudit — 엔티티 공통 필드

| 필드 | BaseTime | BaseAudit |
|------|----------|-----------|
| `createdAt` | ✅ | ✅ |
| `updatedAt` | ✅ | ✅ |
| `deletedAt` | ✅ | ✅ |
| `createdBy` | ❌ | ✅ |
| `updatedBy` | ❌ | ✅ |
| `deletedBy` | ❌ | ✅ |

메인 클래스에 `@EnableJpaAuditing` 추가 필요.

```java
@EnableJpaAuditing
@SpringBootApplication
public class MyServiceApplication { ... }
```

```java
// 생성/수정/삭제 시각만
@Entity
public class Product extends BaseTime { ... }

// 생성/수정/삭제 시각 + 작성자
@Entity
public class Order extends BaseAudit { ... }
```

`BaseAudit`의 `createdBy` / `updatedBy` 자동 주입은 **각 서비스에서 인증 구현 후** `AuditorAware` 빈을 등록해야 합니다.

```java
// 인증/인가 구현 후 각 서비스에서 직접 등록
@Bean
public AuditorAware<String> auditorAware() {
    return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getId);  // JWT 파싱 후 userId 등
}
```

**소프트 삭제:**

```java
// BaseTime 상속
product.softDelete();

// BaseAudit 상속 (삭제자 기록)
order.softDelete(currentUserId);

// 삭제 여부 확인
product.isDeleted();
```

삭제된 데이터 제외 조회:
```java
// Repository 메서드
List<Product> findByDeletedAtIsNull();

// 또는 엔티티에 @Where 선언 (항상 자동 필터링)
@Entity
@Where(clause = "deleted_at IS NULL")
public class Product extends BaseTime { ... }
```

---

### PageRequest / PageResponse — 페이지네이션

허용 사이즈: **10 / 30 / 50** (그 외 값은 자동으로 10으로 대체)

```java
@GetMapping("/products")
public ResponseEntity<ApiResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size).toPageable();
    return ApiResponse.ok(productRepository.findAll(pageable), ProductResponse::from);
}
```

응답 예시:
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false
  }
}
```

---

### ErrorCode / BusinessException — 예외 처리

서비스별로 `ErrorCode` 인터페이스를 구현하는 enum 정의:

```java
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("U002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
```

예외 던지기:

```java
throw new BusinessException(UserErrorCode.USER_NOT_FOUND);

// 상세 메시지 포함
throw new BusinessException(UserErrorCode.USER_NOT_FOUND, "userId=" + userId);
```

`GlobalExceptionHandler`는 라이브러리에 포함되어 **자동으로 등록**됩니다.
커스텀 핸들러가 필요하면 `@RestControllerAdvice` 빈을 직접 등록하면 자동 등록이 비활성화됩니다.

---

### ApiResponse — 공통 응답 구조

```java
// 200 성공
return ApiResponse.ok(response);

// 200 성공 (데이터 없음)
return ApiResponse.ok();

// 201 생성
return ApiResponse.created(response);

// 201 생성 (데이터 없음)
return ApiResponse.created();

// 페이지네이션
return ApiResponse.ok(result, ProductResponse::from);
```

에러는 `GlobalExceptionHandler`가 자동 처리합니다. 직접 반환이 필요한 경우:
```java
return ResponseEntity
    .status(errorCode.getHttpStatus())
    .body(ApiResponse.error(errorCode));
```

---

### TimeUtil — 시간 유틸

```java
// Instant → 서울 시간 LocalDateTime
LocalDateTime ldt = TimeUtil.toLocalDateTime(entity.getCreatedAt());

// 포맷팅
String formatted = TimeUtil.format(entity.getCreatedAt(), "yyyy-MM-dd HH:mm");

// 기간 체크
boolean valid = TimeUtil.isBetween(Instant.now(), event.getStartAt(), event.getEndAt());
```

---

### Kafka 이벤트 (Outbox 패턴)

`spring-kafka` 의존성이 있는 서비스에서만 자동 활성화됩니다.
트랜잭션 커밋 이후 Kafka 발행을 보장하며, 실패 시 자동 재시도합니다.

#### 1. 메인 클래스 설정

```java
@EnableJpaAuditing
@EnableScheduling   // OutboxRelayScheduler 동작에 필요
@SpringBootApplication(scanBasePackages = "com.followMe")
@EntityScan(basePackages = "com.followMe")  // 서비스 엔티티 + Outbox/Inbox 모두 스캔
public class MyServiceApplication { ... }
```

#### 2. 이벤트 클래스 정의

`BaseEvent`를 상속하고 `domainType`(어떤 도메인인지), `domainId`(대상 ID)를 생성자에서 지정합니다.
`eventType`은 클래스명으로 자동 설정되며 Kafka 토픽명으로 사용됩니다.

```java
@Getter
public class OrderCreatedEvent extends BaseEvent {
    private final String customerName;
    private final int totalPrice;

    public OrderCreatedEvent(UUID orderId, String customerName, int totalPrice) {
        super("ORDER", orderId);  // domainType, domainId(UUID 가능)
        this.customerName = customerName;
        this.totalPrice = totalPrice;
    }
}
```

#### 3. 이벤트 발행

`@Transactional` 안에서 `Events.trigger()`를 호출하세요.
트랜잭션이 **커밋된 이후**에 Outbox DB 저장 → Kafka 발행이 순서대로 실행됩니다.

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    @Transactional
    public void createOrder(CreateOrderRequest request) {
        Order order = orderRepository.save(Order.from(request));

        Events.trigger(
            new OrderCreatedEvent(order.getId(), request.getCustomerName(), order.getTotalPrice())
                .withCorrelationId(request.getCorrelationId())  // 선택사항
        );
    }
}
```

> ⚠️ `@Transactional` 없이 호출하면 `OutboxEventListener`가 동작하지 않습니다.

#### 4. 이벤트 소비 (중복 방지 포함)

`InboxRepository`로 동일 이벤트가 중복 처리되는 것을 방지합니다.

```java
@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final InboxRepository inboxRepository;
    private final NotificationService notificationService;

    @Transactional
    @KafkaListener(topics = "OrderCreatedEvent", groupId = "notification-service")
    public void consume(OrderCreatedEvent event) {
        UUID eventId = UUID.fromString(event.getEventId());

        // 중복 처리 방지
        if (inboxRepository.existsByIdAndMessageGroup(eventId, "OrderCreatedEvent")) {
            return;
        }

        notificationService.sendOrderConfirmation(event);

        inboxRepository.save(Inbox.builder()
            .id(eventId)
            .messageGroup("OrderCreatedEvent")
            .build());
    }
}
```

#### 5. application.yml Kafka 설정

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
```

#### 동작 흐름

```
Events.trigger(event)
  → ApplicationEventPublisher (Spring 내부)
    → OutboxEventListener (트랜잭션 커밋 후)
        ├── p_outbox 테이블에 PENDING 저장
        └── Kafka 발행 → 성공: PROCESSED / 실패: FAILED
              ↑
OutboxRelayScheduler (10초마다)
  └── PENDING/FAILED 중 재시도 3회 미만인 것 재발행
```

---

## 버전 히스토리

| 버전 | 변경 내용 |
|------|-----------|
| 1.0.0 | 최초 릴리즈 |