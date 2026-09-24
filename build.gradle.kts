plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    // 최신 LTS. 로컬에 없으면 foojay resolver가 자동으로 받아온다.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- 웹 / 데이터 ---
    // Boot 4부터 spring-boot-starter-web은 deprecated. webmvc를 쓴다.
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // --- API 문서 ---
    implementation(libs.springdoc.webmvc.ui)

    // --- DTO 매핑 ---
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    // --- Lombok (엔티티 보일러플레이트 + @Slf4j 용도로만 제한 사용) ---
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    // Lombok이 생성한 getter를 MapStruct가 인식하게 해주는 바인딩.
    // 빠지면 "Unmapped target properties" 컴파일 에러가 난다.
    annotationProcessor(libs.lombok.mapstruct.binding)

    // --- 런타임 ---
    runtimeOnly("org.postgresql:postgresql")

    // --- 로컬 개발 ---
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    // bootRun 시 compose.yaml의 PostgreSQL을 자동으로 띄운다. (테스트는 Testcontainers 사용)
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    // --- 테스트 ---
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Testcontainers 2.x부터 모듈 아티팩트에 testcontainers- 접두사가 붙었다.
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "-parameters",
            "-Amapstruct.defaultComponentModel=spring",
            "-Amapstruct.unmappedTargetPolicy=ERROR",
        )
    )
}

tasks.withType<Test> {
    useJUnitPlatform()
    // 통합 테스트는 Testcontainers를 쓰므로 Docker가 없으면 자동으로 skip된다.
    testLogging {
        events("passed", "skipped", "failed")
    }
}
