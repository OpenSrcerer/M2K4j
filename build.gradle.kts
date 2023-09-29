import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.2"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
}

group = "online.danielstefani"
version = "0.1.0"
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // ---- Spring ----
    implementation("org.springframework.boot:spring-boot-starter:3.1.4")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.1.4") // only for health check controller lol
    implementation("org.springframework.integration:spring-integration-mqtt:6.1.3")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.1.4")

    // ---- Kotlin ----
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.20")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.20")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")

    // ---- AWS SDK ----
    implementation("software.amazon.awssdk:aws-sdk-java:2.20.80")

    // ---- Serialization ----
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // ---- HiveMQ MQTT Client ----
    implementation("com.hivemq:hivemq-mqtt-client-reactor:1.3.0")

    // ---- Test ----
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.1.4")
    testImplementation("io.projectreactor:reactor-test:3.5.10")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
