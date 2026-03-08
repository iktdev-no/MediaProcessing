plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "no.iktdev.mediaprocessing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven { url = uri("https://reposilite.iktdev.no/releases") }
    maven { url = uri("https://reposilite.iktdev.no/snapshots") }
}

dependencies {

    // Spring Boot (BOM styres globalt i root)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Logging / JSON
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    // Custom libs
    implementation(libs.exfl)
    implementation("no.iktdev.streamit.library:streamit-library-db:1.0.0-alpha14")
    implementation(libs.eventi)

    // Coroutines / utilities
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    // Internal modules
    implementation(project(":transfer-model"))
    implementation(project(":shared:ffmpeg"))
    implementation(project(":shared:common"))
    implementation(project(":shared:database"))

    // Jackson (versjon styres av Spring Boot BOM)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")




    // --- TESTING ---

    // Spring Boot test stack (inkluderer JUnit 5)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.21.0")

    // Mockito (kun moderne versjoner)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    // MockK (moderne versjon)
    testImplementation("io.mockk:mockk:1.13.9")

    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // JSON testing
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    // Test (samme versjon, BOM-styrt)
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Internal test artifacts
    testImplementation(project(":shared:common", configuration = "testArtifacts"))
    testImplementation(project(":shared:database", configuration = "testArtifacts"))

    // Exposed test
    val exposedVersion = "0.61.0"
    testImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("app.jar")
    launchScript()
}

tasks.jar {
    archiveFileName.set("app.jar")
    archiveBaseName.set("app")
}
