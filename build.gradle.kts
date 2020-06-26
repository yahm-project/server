import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.spring") version "1.3.71"
    application
}

group = "it.unibo.yahm"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.neo4j.springframework.data:spring-data-neo4j-rx-spring-boot-starter:1.0.0-rc01")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.4")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.neo4j.test:neo4j-harness:4.1.0") {
        exclude(group = "org.slf4j", module = "slf4j-nop")
    }
    testImplementation("org.neo4j.springframework.data:spring-data-neo4j-rx-spring-boot-test-autoconfigure:1.1.1")
    testImplementation("org.testcontainers:junit-jupiter:1.13.0")
    testImplementation("org.testcontainers:neo4j:1.13.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

application {
    mainClassName = "it.unibo.yahm.server.ServerApplicationKt"
}
