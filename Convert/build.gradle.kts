import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    kotlin("jvm") version "1.8.21"
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("plugin.spring") version "1.5.31"
}

group = "no.iktdev.streamit.content"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        url = uri("https://reposilite.iktdev.no/releases")
    }
    maven {
        url = uri("https://reposilite.iktdev.no/snapshots")
    }
}
dependencies {
    implementation(project(":CommonCode"))

    implementation("no.iktdev.library:subtitle:1.7.8-SNAPSHOT")

    implementation("no.iktdev.streamit.library:streamit-library-kafka:0.0.2-alpha84")
    implementation("no.iktdev:exfl:0.0.13-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    implementation("com.github.pgreze:kotlin-process:1.3.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    implementation("com.google.code.gson:gson:2.8.9")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter:2.7.0")
    implementation("org.springframework.kafka:spring-kafka:2.8.5")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")



    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("converter.jar")
    launchScript()
}

tasks.jar {
    archivesName.set("converter.jar")
    archiveBaseName.set("converter")
}
archivesName.set("converter.jar")