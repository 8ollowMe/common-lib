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
   - [Kafka 이벤트 (BaseEvent / KafkaEventPublisher)](#46-kafka-이벤트-baseevent--kafkaeventpublisher)
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
    │   └── KafkaEventPublisher.java            # Kafka 발행 유틸
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

### 4.6 Kafka 이벤트 (BaseEvent / KafkaEventPublisher)

#### 이벤트 클래스 정의

```java
@Getter
public class UserCreatedEvent extends BaseEvent {
    private final Long userId;
    private final String email;

    public UserCreatedEvent(Long userId, String email) {
        super("USER_CREATED");   // eventType 지정
        this.userId = userId;
        this.email = email;
    }
}
```

`BaseEvent`는 `eventId`(UUID), `eventType`, `occurredAt`(Instant)을 자동 생성합니다.

#### 이벤트 발행

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final KafkaEventPublisher eventPublisher;

    public void createUser(...) {
        // ... 비즈니스 로직
        eventPublisher.publish("user.created", new UserCreatedEvent(user.getId(), user.getEmail()));
    }
}
```

파티션 키는 `eventId`(UUID)로 자동 설정됩니다. 특정 키가 필요하면:

```java
eventPublisher.publish("user.created", String.valueOf(userId), event);
```

#### 소비자 서비스 application.yml 설정

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

## 5. Auto Configuration

`spring-boot-autoconfigure`를 통해 **별도 `@Bean` 선언 없이** 자동으로 등록됩니다.

| 설정 클래스 | 등록되는 빈 | 활성화 조건 |
|---|---|---|
| `CommonWebAutoConfiguration` | `GlobalExceptionHandler` | `spring-webmvc` 클래스패스에 존재할 때 |
| `CommonKafkaAutoConfiguration` | `KafkaEventPublisher` | `spring-kafka` 클래스패스에 존재할 때 |

Kafka를 사용하지 않는 서비스에서는 `spring-kafka` 의존성이 없으면 `KafkaEventPublisher`가 등록되지 않으므로 충돌 없이 사용할 수 있습니다.