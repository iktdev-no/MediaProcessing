plugins {
    id("java")                                     // core plugin – aktiv i root
    kotlin("jvm") version "2.2.0"                  // Kotlin i root
    kotlin("plugin.spring") version "2.2.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0" apply false
    id("org.springframework.boot") version "3.4.1" apply false
}

group = "no.iktdev.mediaprocessing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven { url = uri("https://reposilite.iktdev.no/releases") }
    maven { url = uri("https://reposilite.iktdev.no/snapshots") }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
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
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

}

