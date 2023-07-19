import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    kotlin("jvm") version "1.8.21"
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("plugin.spring") version "1.5.31"
}

archivesName.set("reader.jar")
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
    implementation("no.iktdev.streamit.library:streamit-library-kafka:0.0.2-alpha34")
    implementation("no.iktdev:exfl:0.0.4-SNAPSHOT")

    implementation("com.github.pgreze:kotlin-process:1.3.1")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter:2.7.0")
    implementation("org.springframework.kafka:spring-kafka:2.8.5")

    implementation(project(":CommonCode"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.assertj:assertj-core:3.4.1")


}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("reader.jar")
    launchScript()
}

tasks.jar {
    archivesName.set("reader.jar")
    archiveBaseName.set("reader")
}