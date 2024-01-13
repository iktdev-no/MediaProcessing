plugins {
    id("java")
    kotlin("jvm") version "1.9.20"
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

val exposedVersion = "0.44.0"
dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("com.github.pgreze:kotlin-process:1.3.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("no.iktdev:exfl:0.0.13-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20230227")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation ("mysql:mysql-connector-java:8.0.29")
    implementation(project(mapOf("path" to ":shared:kafka")))


    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}