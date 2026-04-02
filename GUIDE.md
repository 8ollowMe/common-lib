# common-lib 개발 가이드

> MSA 환경에서 각 서비스가 공통으로 쓰는 코드를 한 곳에 모아 관리하는 라이브러리입니다.
> 응답 구조, 예외 처리, 엔티티 공통 필드, 페이지네이션, 시간 유틸, Kafka 이벤트 발행을 제공합니다.

---

## 목차

1. [프로젝트 구조](#1-프로젝트-구조)
2. [빌드 & 배포](#2-빌드--배포)
3. [소비자 서비스에서 의존성 추가](#3-소비자-서비스에서-의존성-추가)
4. [모듈별 설명](#4-모듈별-설명)
   - [응답 구조 (ApiResponse)](#41-응답-구조-apiresponse)
   - [예외 처리 (Exception)](#42-예외-처리-exception)
   - [공통 엔티티 (BaseTime / BaseAudit)](#43-공통-엔티티-basetime--baseaudit)
   - [페이지네이션 (PageRequest / PageResponse)](#44-페이지네이션-pagerequest--pageresponse)
   - [시간 유틸 (TimeUtil)](#45-시간-유틸-timeutil)
   - [Kafka 이벤트 (Outbox 패턴)](#46-kafka-이벤트-outbox-패턴)
5. [Auto Configuration](#5-auto-configuration)

---

## 1. 프로젝트 구조

```
common-lib
├── build.gradle.kts                        # 라이브러리 빌드 설정 (java-library + maven-publish)
├── settings.gradle.kts
└── src/main/java/com/followMe/common/
    ├── autoconfigure/
    │   ├── CommonWebAutoConfiguration.java     # GlobalExceptionHandler 자동 등록
    │   └── CommonKafkaAutoConfiguration.java   # KafkaEventPublisher 자동 등록
    ├── entity/
    │   ├── BaseTime.java                       # 생성/수정 시각 공통 엔티티
    │   └── BaseAudit.java                      # BaseTime + 작성자 정보
    ├── event/
    │   ├── BaseEvent.java                      # Kafka 이벤트 기반 클래스
    │   ├── Events.java                         # 이벤트 발행 정적 유틸 (DDD 스타일)
    │   ├── exception/
    │   │   └── EventPublishFailureEvent.java   # 이벤트 발행 실패 예외
    │   ├── outbox/
    │   │   ├── Outbox.java                     # Outbox 엔티티 (p_outbox 테이블)
    │   │   ├── OutboxEvent.java                # Spring 내부 이벤트 봉투
    │   │   ├── OutboxStatus.java               # PENDING / PROCESSED / FAILED
    │   │   ├── OutboxRepository.java
    │   │   └── OutboxEventListener.java        # 트랜잭션 커밋 후 Kafka 발행
    │   ├── inbox/
    │   │   ├── Inbox.java                      # Inbox 엔티티 (p_inbox 테이블)
    │   │   └── InboxRepository.java
    │   └── scheduler/
    │       └── OutboxRelayScheduler.java       # PENDING/FAILED 재발행 (10초마다)
    ├── exception/
    │   ├── ErrorCode.java                      # 에러코드 인터페이스
    │   ├── CommonErrorCode.java                # 공통 에러코드 enum
    │   └── BusinessException.java              # 비즈니스 예외
    ├── pagination/
    │   ├── PageRequest.java                    # 페이지 요청 DTO
    │   └── PageResponse.java                   # 페이지 응답 DTO
    ├── response/
    │   ├── ApiResponse.java                    # 표준 API 응답 래퍼
    │   └── ErrorResponse.java                  # 에러 응답 DTO
    └── util/
        └── TimeUtil.java                       # Instant ↔ LocalDateTime 변환 유틸
```

---

## 2. 빌드 & 배포

### 로컬 Maven 저장소에 배포 (개발 중 테스트)

```bash
./gradlew publishToMavenLocal
# ~/.m2/repository/com/followMe/common-lib/{version}/ 에 설치됩니다
```

### GitHub Packages에 배포 (팀 공유)

`~/.gradle/gradle.properties` 에 GitHub 인증 정보를 추가합니다.

```properties
gpr.user=<GitHub 사용자명>
gpr.key=<GitHub Personal Access Token>
```

```bash
./gradlew publish
```

---

## 3. 소비자 서비스에서 의존성 추가

### GitHub Packages를 통해 사용하는 경우

`build.gradle`에 다음을 추가합니다.

```groovy
repositories {
    maven {
        url "https://maven.pkg.github.com/8ollowMe/common-lib"
        credentials {
            username = findProperty("gpr.user")
            password = findProperty("gpr.key")
        }
    }
}

dependencies {
    implementation 'com.followMe:common-lib:{version}'  // 최신 버전은 GitHub Packages에서 확인
}
```

### 로컬 테스트용 (mavenLocal)

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation 'com.followMe:common-lib:{version}'  // 최신 버전은 GitHub Packages에서 확인
}
```

---

## 4. 모듈별 설명

### 4.1 응답 구조 (ApiResponse)

모든 API 응답을 `{ success, data, error }` 구조로 통일합니다.

```java
// 단순 성공
return ApiResponse.ok(data);

// 생성 성공 (201)
return ApiResponse.created(savedEntity);

// 페이지 응답
return ApiResponse.ok(page);                         // Page<T> 그대로
return ApiResponse.ok(page, EntityDto::from);        // 매핑 함수 적용
```

**응답 JSON 예시**

```json
// 성공
{ "success": true, "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null, "error": { "code": "INVALID_INPUT", "message": "..." } }
```

---

### 4.2 예외 처리 (Exception)

#### ErrorCode 인터페이스 구현 (서비스별 커스텀 에러코드)

```java
@Getter
@RequiredArgsConstructor
public enum MyErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("USER_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
```

#### BusinessException 던지기

```java
// 직접 생성
throw new BusinessException(MyErrorCode.USER_NOT_FOUND);

// ErrorCode의 toException() 사용
throw MyErrorCode.USER_NOT_FOUND.toException();
```

`GlobalExceptionHandler`가 자동으로 잡아서 `ApiResponse.error(...)` 형태로 응답합니다.
별도 `@ExceptionHandler` 작성 없이 바로 사용할 수 있습니다.

---

### 4.3 공통 엔티티 (BaseTime / BaseAudit)

엔티티에 공통 시간/작성자 필드를 상속으로 추가합니다.

#### BaseTime — 생성/수정 시각만 필요할 때

```java
@Entity
public class Post extends BaseTime {
    // createdAt, updatedAt, deletedAt, isDeleted 자동 포함
}
```

소비자 서비스 메인 클래스에 `@EnableJpaAuditing` 선언 필요.

```java
@EnableJpaAuditing
@SpringBootApplication
public class MyServiceApplication { ... }
```

#### BaseAudit — 작성자 정보까지 필요할 때

```java
@Entity
public class Post extends BaseAudit {
    // BaseTime 필드 + createdBy, updatedBy, deletedBy 자동 포함
}
```

`AuditorAware<String>` 빈도 함께 등록해야 합니다.

```java
@Bean
public AuditorAware<String> auditorAware() {
    return () -> Optional.ofNullable(SecurityContextHolder.getContext())
        .map(ctx -> ctx.getAuthentication().getName());
}
```

#### 소프트 삭제

```java
post.softDelete();           // BaseTime — isDeleted=true, deletedAt 기록
post.softDelete("userId");   // BaseAudit — 위 + deletedBy 기록
```

---

### 4.4 페이지네이션 (PageRequest / PageResponse)

#### 컨트롤러에서 요청 받기

```java
@GetMapping("/posts")
public ResponseEntity<ApiResponse> getPosts(@ModelAttribute PageRequest pageRequest) {
    Page<Post> page = postService.getList(pageRequest.toPageable());
    return ApiResponse.ok(page, PostDto::from);
}
```

`PageRequest` 기본값: `page=0`, `size=20`, `sort=createdAt`, `direction=DESC`

#### PageResponse 구조

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

---

### 4.5 시간 유틸 (TimeUtil)

DB/도메인 레이어는 `Instant`(UTC)로 저장하고, 표현 레이어에서 한국 시간으로 변환하는 패턴을 권장합니다.

```java
// Instant → 서울 시간대 LocalDateTime
LocalDateTime ldt = TimeUtil.toLocalDateTime(entity.getCreatedAt());

// LocalDateTime → Instant
Instant instant = TimeUtil.toInstant(localDateTime);

// 포맷팅 (기본: "yyyy-MM-dd HH:mm:ss", 서울 시간대)
String formatted = TimeUtil.format(entity.getCreatedAt());

// 현재 시각 (서울 기준)
LocalDateTime now = TimeUtil.nowSeoul();
```

---

### 4.6 Kafka 이벤트 (Outbox 패턴)

트랜잭션 커밋 이후 Kafka 발행을 보장합니다. 발행 실패 시 10초마다 자동 재시도(최대 3회)합니다.

#### 소비자 서비스 메인 클래스 설정

```java
@EnableJpaAuditing
@EnableScheduling   // OutboxRelayScheduler 동작에 필요
@SpringBootApplication
@EntityScan(basePackages = "com.followMe")  // 서비스 엔티티 + Outbox/Inbox 모두 스캔
public class MyServiceApplication { ... }
```

#### 1단계: 이벤트 클래스 정의

`BaseEvent`를 상속하고 생성자에서 `domainType`과 `domainId`를 지정합니다.
`eventType`은 **클래스명이 자동으로 설정**되며 Kafka 토픽명으로 사용됩니다.

```java
@Getter
public class UserCreatedEvent extends BaseEvent {
    private final String email;
    private final String username;

    public UserCreatedEvent(UUID userId, String email, String username) {
        super("USER", userId);   // domainType, domainId (UUID 직접 전달 가능)
        this.email = email;
        this.username = username;
    }
}
```

| 필드 | 설명 | 설정 방법 |
|---|---|---|
| `eventId` | 이벤트 고유 UUID | 자동 생성 |
| `eventType` | Kafka 토픽명 | 클래스명 자동 설정 (`UserCreatedEvent`) |
| `domainType` | 어떤 도메인의 이벤트인지 | 생성자에서 지정 (`"USER"`) |
| `domainId` | 대상 엔티티 ID | 생성자에서 지정 (UUID or String) |
| `occurredAt` | 발생 시각 (UTC) | 자동 생성 |
| `correlationId` | 분산 추적용 ID | `.withCorrelationId()` 선택 설정 |

#### 2단계: 이벤트 발행

반드시 `@Transactional` 안에서 `Events.trigger()`를 호출해야 합니다.

```java
@Service
@RequiredArgsConstructor
public class UserService {

    @Transactional
    public void createUser(CreateUserRequest request) {
        User user = userRepository.save(User.from(request));

        // 기본 발행
        Events.trigger(new UserCreatedEvent(user.getId(), user.getEmail(), user.getUsername()));

        // correlationId 포함 (분산 추적이 필요한 경우)
        Events.trigger(
            new UserCreatedEvent(user.getId(), user.getEmail(), user.getUsername())
                .withCorrelationId(request.getCorrelationId())
        );
    }
}
```

> ⚠️ `@Transactional` 없이 호출하면 `OutboxEventListener`가 동작하지 않아 이벤트가 발행되지 않습니다.

#### 3단계: 이벤트 소비 (중복 방지 포함)

`InboxRepository`로 동일 이벤트의 중복 처리를 방지합니다.

```java
@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final InboxRepository inboxRepository;

    @Transactional
    @KafkaListener(topics = "UserCreatedEvent", groupId = "notification-service")
    public void consume(UserCreatedEvent event) {
        UUID eventId = UUID.fromString(event.getEventId());

        // 중복 처리 방지
        if (inboxRepository.existsByIdAndMessageGroup(eventId, "UserCreatedEvent")) {
            return;
        }

        // 비즈니스 로직 처리
        notificationService.sendWelcomeMail(event.getEmail());

        // 처리 완료 기록
        inboxRepository.save(Inbox.builder()
            .id(eventId)
            .messageGroup("UserCreatedEvent")
            .build());
    }
}
```

#### 동작 흐름

```
Events.trigger(event)                         // 1. 도메인 서비스에서 호출
  → ApplicationEventPublisher                 // 2. Spring 내부 채널로 전달
    → OutboxEventListener (AFTER_COMMIT)       // 3. 트랜잭션 커밋 이후 실행
        ├── p_outbox 테이블에 PENDING 저장     // 4. DB에 기록
        └── Kafka 발행                         // 5. 발행 시도
            ├── 성공 → PROCESSED
            └── 실패 → FAILED
                  ↑
OutboxRelayScheduler (10초마다)                // 6. 실패 건 자동 재시도 (최대 3회)
```

#### application.yml Kafka 직렬화 설정

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
```

---

#Auto Configuration

`spring-boot-autoconfigure`를 통해 **별도 `@Bean` 선언 없이** 자동으로 등록됩니다.

| 설정 클래스 | 등록되는 빈 | 활성화 조건 |
|---|---|---|
| `CommonWebAutoConfiguration` | `GlobalExceptionHandler`, Argument Resolvers | `spring-webmvc` 클래스패스에 존재할 때 |
| `CommonKafkaAutoConfiguration` | `KafkaEventPublisher` | `spring-kafka` 클래스패스에 존재할 때 |
| `CommonEventAutoConfiguration` | `Events`, `OutboxEventListener`, `OutboxRelayScheduler` | `spring-kafka` 클래스패스에 존재할 때 |

`spring-kafka` 의존성이 없으면 Kafka 관련 빈이 등록되지 않으므로 Kafka를 쓰지 않는 서비스에서는 충돌 없이 사용할 수 있습니다.