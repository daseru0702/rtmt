plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "app.rtmt"
version = "0.0.1-SNAPSHOT"
description = "rtmt"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- Web / Reactive ---
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // 선택: 보안 사용 시 (현재 permitAll이면 유지/제거 선택 가능)
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- Redis (Reactive) ---
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // --- R2DBC + MySQL 드라이버/풀 ---
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.asyncer:r2dbc-mysql:1.3.2")
    implementation("io.r2dbc:r2dbc-pool:1.0.1.RELEASE")

    // --- Flyway (JDBC 경로로 마이그레이션) ---
    implementation("org.flywaydb:flyway-core:11.7.2")
    implementation("org.flywaydb:flyway-mysql:11.7.2")
    // JDBC 드라이버는 런타임에만 필요
    runtimeOnly("com.mysql:mysql-connector-j:8.4.0")

    // --- Kotlin / Reactor ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // --- Test ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
