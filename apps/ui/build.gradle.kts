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
    implementation(kotlin("stdlib-jdk8"))


    implementation("org.springframework.boot:spring-boot-starter-web:3.0.4")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")


    implementation("no.iktdev:exfl:0.0.13-SNAPSHOT")
    implementation(project(mapOf("path" to ":shared")))
    implementation(project(mapOf("path" to ":shared:common")))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}