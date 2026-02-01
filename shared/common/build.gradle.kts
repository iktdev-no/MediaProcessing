plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "no.iktdev.mediaprocessing.shared"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        name = "ReposiliteReleases"
        url = uri("https://reposilite.iktdev.no/releases")
    }
    maven {
        name = "ReposiliteSnapshot"
        url = uri("https://reposilite.iktdev.no/snapshots")
    }
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")


    implementation("com.github.pgreze:kotlin-process:1.3.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation(libs.exfl)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20231013")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")


    implementation("org.apache.commons:commons-lang3:3.12.0")

    implementation(project(":shared:ffmpeg"))
    implementation(libs.eventi)

    implementation("com.ibm.icu:icu4j:75.1")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("org.assertj:assertj-core:3.24.2")

    testImplementation("io.kotest:kotest-assertions-core:5.7.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    testImplementation("io.github.classgraph:classgraph:4.8.184")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    testImplementation("org.reflections:reflections:0.10.2")



}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

configurations { create("testArtifacts") }

tasks.register<Jar>("testJar") {
    from(sourceSets.test.get().output)
    archiveClassifier.set("tests")
}

artifacts {
    add("testArtifacts", tasks.named("testJar"))
}
