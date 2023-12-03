plugins {
    id("java")
    kotlin("jvm")
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

    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter:2.7.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")
    implementation(project(mapOf("path" to ":shared")))

    implementation("no.iktdev:exfl:0.0.13-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
    implementation(project(mapOf("path" to ":shared:kafka")))

    implementation("org.springframework.kafka:spring-kafka:3.0.1")
    implementation(project(mapOf("path" to ":shared:contract")))




    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}