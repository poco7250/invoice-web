plugins {
    // JDK 툴체인 자동 다운로드 (로컬에 JDK 25가 없어도 Gradle이 받아서 씀)
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "claude-spring-starterkit"
