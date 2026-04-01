plugins {
    `java-library`
    `maven-publish`
}

group = "com.followMe"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

// Spring Boot BOM을 version catalog로 관리 (소비자에게 강제하지 않음)
val springBootVersion = "3.5.1"
val lombokVersion = "1.18.34"
val querydslVersion = "5.1.0"

dependencies {
    // ── BOM (소비자에게 전이되지 않음, 라이브러리 컴파일용) ──────────────────
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    // ── Lombok ──────────────────────────────────────────────────────────────
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // ── JPA (소비자가 Spring Data JPA 를 사용한다고 가정) ────────────────────
    compileOnly("jakarta.persistence:jakarta.persistence-api")
    compileOnly("org.springframework.data:spring-data-commons")
    compileOnly("org.springframework.data:spring-data-jpa")

    // ── QueryDSL (Q 슈퍼타입 클래스 생성 — QBaseTime, QBaseAudit) ───────────
    compileOnly("com.querydsl:querydsl-jpa:$querydslVersion:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:$querydslVersion:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")
    api ("com.querydsl:querydsl-core:$querydslVersion")

    // ── Spring Web/MVC (GlobalExceptionHandler 용) ───────────────────────────
    compileOnly("org.springframework:spring-web")
    compileOnly("org.springframework:spring-webmvc")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("jakarta.validation:jakarta.validation-api")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    // ── Jackson (ApiResponse JSON 직렬화) ────────────────────────────────────
    // api → 소비자도 이 타입을 직접 쓰므로 transitive 허용
    api("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.1")

    // ── Kafka (선택적 — 소비자가 spring-kafka 없으면 KafkaAutoConfig 비활성) ──
    compileOnly("org.springframework.kafka:spring-kafka")

    // ── Test ─────────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            versionMapping {
                usage("java-api") { fromResolutionOf("runtimeClasspath") }
                usage("java-runtime") { fromResolutionResult() }
            }
            pom {
                name = "Common Library"
                description = "MSA 공통 라이브러리 (Entity, Pagination, Exception, Response, Kafka Event)"
            }
        }
    }
    repositories {
        // 로컬 Maven 저장소 (~/.m2) 에 배포
        mavenLocal()
        // providers.gradleProperty()로 읽어야 non-null String이 보장되어
        // Gradle 9의 credentials.username 검증을 통과함
        // CI에서는 ORG_GRADLE_PROJECT_* 환경변수로 주입, 없으면 저장소 자체를 추가 안 함
        val githubUser = providers.gradleProperty("GitHubPackagesUsername").orNull
        val githubPass = providers.gradleProperty("GitHubPackagesPassword").orNull
        println(">>> [DEBUG] GITHUB_ACTOR env       = ${System.getenv("GITHUB_ACTOR")}")
        println(">>> [DEBUG] GITHUB_TOKEN env        = ${if (System.getenv("GITHUB_TOKEN") != null) "SET(${System.getenv("GITHUB_TOKEN")!!.length}chars)" else "NULL"}")
        println(">>> [DEBUG] GitHubPackagesUsername  = $githubUser")
        println(">>> [DEBUG] GitHubPackagesPassword  = ${if (githubPass != null) "SET(${githubPass.length}chars)" else "NULL"}")
        if (githubUser != null && githubPass != null) {
            println(">>> [DEBUG] GitHubPackages 저장소 추가됨 → PublishToMavenRepository task 생성")
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/8ollowMe/common-lib")
                credentials {
                    username = githubUser
                    password = githubPass
                }
            }
        } else {
            println(">>> [DEBUG] GitHubPackages 저장소 건너뜀 (프로퍼티 없음) → task 미생성")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}