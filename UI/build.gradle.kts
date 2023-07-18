plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
}

base.archivesBaseName = "ui"

group = "no.iktdev.streamit.content"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-graphql:2.7.4")
    implementation("org.springframework.boot:spring-boot-starter-web:3.0.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")
}

tasks.test {
    useJUnitPlatform()
}