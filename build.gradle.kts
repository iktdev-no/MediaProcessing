plugins {
    id("java")                                     // core plugin – aktiv i root
    kotlin("jvm") version "2.2.0"                  // Kotlin i root
    kotlin("plugin.spring") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false
    id("org.springframework.boot") version "3.4.1" apply false
    id("no.iktdev.ts-gen") version "1.0-rc7" apply false
    id("ch.acanda.gradle.fabrikt") version "1.18.0" apply false
}

group = "no.iktdev.mediaprocessing"
version = "1.0-SNAPSHOT"

val coroutinesCoreVersion = "1.7.1"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://jitpack.io")
    maven { url = uri("https://reposilite.iktdev.no/releases") }
    maven { url = uri("https://reposilite.iktdev.no/snapshots") }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
        maven { url = uri("https://reposilite.iktdev.no/releases") }
        maven { url = uri("https://reposilite.iktdev.no/snapshots") }
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.1"))
        implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
        implementation("com.google.code.gson:gson:2.8.9")
        implementation("org.json:json:20231013")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesCoreVersion")
        implementation("org.lz4:lz4-java:1.8.0")

    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

}

tasks.test {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

tasks.register("prepare") {
    group = "application"
    description = "Prepares the UI models and TypeScript contracts for development."
    dependsOn(":apps:ui:generateTs")
}
