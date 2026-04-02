# common-lib 개발 가이드

> MSA 환경에서 각 서비스가 공통으로 쓰는 코드를 한 곳에 모아 관리하는 라이브러리입니다.
> 응답 구조, 예외 처리, 엔티티 공통 필드, 페이지네이션, 시간 유틸, Kafka 이벤트 발행, Swagger 자동 설정, MDC 로깅을 제공합니다.

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
   - [Swagger / OpenAPI 자동 설정](#47-swagger--openapi-자동-설정)
   - [MDC 로깅](#48-mdc-로깅)
5. [Auto Configuration](#5-auto-configuration)

---

## 1. 프로젝트 구조

```
common-lib
├── build.gradle.kts                        # 라이브러리 빌드 설정 (java-library + maven-publish)
├── settings.gradle.kts
└── src/main/
    ├── resources/
    │   ├── logback-base.xml                    # 공통 logback 기반 설정 (콘솔 + Kafka Appender)
    │   └── META-INF/spring/
    │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── java/com/followMe/common/
        ├── autoconfigure/
        │   ├── CommonWebAutoConfiguration.java     # GlobalExceptionHandler, MdcLoggingFilter, MdcTaskDecorator 자동 등록
        │   ├── CommonKafkaAutoConfiguration.java   # KafkaEventPublisher 자동 등록
        │   ├── CommonEventAutoConfiguration.java   # Events, OutboxEventListener, OutboxRelayScheduler 자동 등록
        │   └── CommonSwaggerAutoConfiguration.java # Bearer 스키마, 페이지네이션 파라미터 자동 등록
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
        │   ├── PageRequest.java                    # 페이지 요청 DTO (허용 사이즈: 10/30/50)
        │   ├── PageResponse.java                   # 페이지 응답 DTO
        │   ├── CursorRequest.java                  # 커서 기반 페이지 요청 DTO
        │   └── CursorResponse.java                 # 커서 기반 페이지 응답 DTO
        ├── response/
        │   ├── ApiResponse.java                    # 표준 API 응답 래퍼
        │   └── ErrorResponse.java                  # 에러 응답 DTO
        ├── util/
        │   ├── TimeUtil.java                       # Instant ↔ LocalDateTime 변환 유틸
        │   └── MdcTaskDecorator.java               # 비동기 스레드 MDC 전파 데코레이터
        └── web/
            ├── GlobalExceptionHandler.java         # 공통 예외 핸들러
            └── MdcLoggingFilter.java               # 요청마다 MDC 추적 정보 주입 필터
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

---

### 4.7 Swagger / OpenAPI 자동 설정

`springdoc-openapi-starter-webmvc-ui` 의존성이 클래스패스에 있으면 자동으로 활성화됩니다.

#### 소비자 서비스 의존성 추가

```kotlin
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")
```

#### 자동으로 등록되는 것들

**Bearer JWT 인증 스키마** — Swagger UI 우상단 자물쇠 버튼으로 토큰 입력 가능.

엔드포인트에 인증이 필요하다면 컨트롤러에 아래 어노테이션을 추가합니다.

```java
@SecurityRequirement(name = "bearerAuth")
@GetMapping("/me")
public ResponseEntity<ApiResponse> getMyInfo() { ... }
```

전체 컨트롤러에 적용하려면 클래스 레벨에 선언합니다.

```java
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
public class UserController { ... }
```

**PageRequest / CursorRequest 파라미터 자동 문서화** — Swagger가 커스텀 타입을 인식하지 못하는 문제를 자동으로 해결합니다.

| 파라미터 타입 | Swagger 표시 |
|---|---|
| `PageRequest` | `page` (integer), `size` (enum: 10/30/50) |
| `CursorRequest` | `cursor` (string), `size` (enum: 10/30/50) |

#### 서비스별 API 정보 설정

제목, 버전, 설명은 서비스마다 다르므로 각 서비스에서 직접 설정합니다.

```java
@Bean
public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("User Service API")
            .version("v1.0.0")
            .description("사용자 관련 API"));
}
```

---

### 4.8 MDC 로깅

모든 HTTP 요청에 추적 정보를 자동으로 주입합니다. ELK 스택과 연동해 요청 단위로 로그를 추적할 수 있습니다.

#### 주입되는 MDC 필드

| 필드 | 설명 | 출처 |
|---|---|---|
| `traceId` | 요청 추적 ID | `X-Trace-Id` 헤더 또는 UUID 8자리 자동 생성 |
| `userId` | 요청한 사용자 ID | `X-User-Id` 헤더 (API Gateway 주입) |
| `method` | HTTP 메서드 | 요청에서 자동 추출 |
| `uri` | 요청 URI | 요청에서 자동 추출 |

`MdcLoggingFilter`가 `Ordered.HIGHEST_PRECEDENCE`로 등록되어 있어 모든 필터보다 먼저 실행됩니다. 응답 헤더에 `X-Trace-Id`를 되돌려줘 클라이언트도 추적 ID를 확인할 수 있습니다.

#### logback-spring.xml 설정 (ELK 연동)

먼저 각 서비스에 의존성을 추가합니다.

```kotlin
implementation("com.github.danielwegener:logback-kafka-appender:0.2.0-RC2")
implementation("net.logstash.logback:logstash-logback-encoder:8.0")
```

그 다음 `src/main/resources/logback-spring.xml`을 작성합니다.

```xml
<configuration>
    <include resource="logback-base.xml"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_KAFKA"/>
    </root>
</configuration>
```

환경 변수로 Kafka 연결 정보를 주입합니다.

```yaml
# application.yml
spring:
  application:
    name: user-service   # 로그의 service_name 필드로 사용됩니다
```

```bash
# 환경 변수
BOOTSTRAP_SERVERS=kafka:9092
LOG_KAFKA_TOPIC=app-logs
```

로그 파이프라인:

```
서비스 → Kafka(app-logs 토픽) → Logstash → Elasticsearch → Kibana
```

#### @Async 사용 시 MDC 전파

`@Async`나 스레드 풀을 쓰면 MDC가 자식 스레드에 전달되지 않습니다.
`MdcTaskDecorator`를 executor에 등록하면 자동으로 전파됩니다.

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor asyncExecutor(MdcTaskDecorator mdcTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setTaskDecorator(mdcTaskDecorator);
        executor.initialize();
        return executor;
    }
}
```

---

## 5. Auto Configuration

`spring-boot-autoconfigure`를 통해 **별도 `@Bean` 선언 없이** 자동으로 등록됩니다.

| 설정 클래스 | 등록되는 빈 | 활성화 조건 |
|---|---|---|
| `CommonWebAutoConfiguration` | `GlobalExceptionHandler`, `MdcLoggingFilter`, `MdcTaskDecorator`, Argument Resolvers | `spring-webmvc` 클래스패스에 존재할 때 |
| `CommonKafkaAutoConfiguration` | `KafkaEventPublisher` | `spring-kafka` 클래스패스에 존재할 때 |
| `CommonEventAutoConfiguration` | `Events`, `OutboxEventListener`, `OutboxRelayScheduler` | `spring-kafka` 클래스패스에 존재할 때 |
| `CommonSwaggerAutoConfiguration` | Bearer 인증 스키마, 페이지네이션 파라미터 커스터마이저 | `springdoc-openapi` 클래스패스에 존재할 때 |

각 설정 클래스에 `@ConditionalOnMissingBean`이 적용되어 있어, 서비스에서 동일한 빈을 직접 등록하면 자동 등록이 비활성화됩니다.