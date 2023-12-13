plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring") version "1.5.31"
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "no.iktdev.mediaprocessing"
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

    /*Spring boot*/
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter:2.7.0")
    // implementation("org.springframework.kafka:spring-kafka:3.0.1")
    implementation("org.springframework.kafka:spring-kafka:2.8.5")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")

    implementation("no.iktdev:exfl:0.0.13-SNAPSHOT")

    implementation(project(mapOf("path" to ":shared:kafka")))
    implementation(project(mapOf("path" to ":shared")))

    implementation(project(mapOf("path" to ":shared:contract")))
    implementation(project(mapOf("path" to ":shared:common")))
}

tasks.test {
    useJUnitPlatform()
}