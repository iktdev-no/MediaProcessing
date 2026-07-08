plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.springframework.boot")
}

group = "no.iktdev.mediaprocessing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven { url = uri("https://reposilite.iktdev.no/releases") }
    maven { url = uri("https://reposilite.iktdev.no/snapshots") }
}

val exposedVersion = "0.61.0"

dependencies {
    // Spring Boot BOM styres globalt
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("no.iktdev:process-runner:1.0.0")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation(libs.exfl)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20231013")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("com.h2database:h2:2.2.220")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.xerial:sqlite-jdbc:3.43.2.0")
        implementation("com.zaxxer:HikariCP:7.0.2")

    implementation(project(":shared:ffmpeg"))
    implementation(project(":shared:common"))
    implementation(libs.eventi)

    // Jackson aligned
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Test

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    testImplementation("io.github.classgraph:classgraph:4.8.184")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.13.9")

    testImplementation(project(":shared:common", configuration = "testArtifacts"))
}

tasks.test {
    useJUnitPlatform()
}

configurations { create("testArtifacts") }

tasks.register<Jar>("testJar") {
    from(sourceSets.test.get().output)
    archiveClassifier.set("tests")
}

artifacts {
    add("testArtifacts", tasks.named("testJar"))
}
