plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "no.iktdev.mediaprocessing.apps"
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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework:spring-tx")

    // implementation("org.springframework.kafka:spring-kafka:3.0.1")


    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    implementation(libs.exfl)
    implementation(libs.eventi)


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
    implementation("com.github.pgreze:kotlin-process:1.4.1")

    //implementation(project(mapOf("path" to ":shared")))
    implementation(project(mapOf("path" to ":shared:ffmpeg")))
    implementation(project(mapOf("path" to ":shared:common")))


    implementation(kotlin("stdlib-jdk8"))

    // --- Spring Boot test stack (inkluderer JUnit 5.10, Mockito, AssertJ, etc.) ---
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito") // valgfritt hvis du kun bruker MockK
    }

    // --- MockK (Kotlin mocking) ---
    testImplementation("io.mockk:mockk:1.13.8")

    // --- Coroutines test utilities ---
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // --- H2 for database testing ---
    testImplementation("com.h2database:h2:2.2.224")

    // --- Optional: AssertJ (Spring Boot inkluderer AssertJ, men du kan eksplisitt legge til) ---
    // testImplementation("org.assertj:assertj-core:3.24.2")

    // --- Optional: JUnit params (brukes ofte) ---
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    // --- Hvis du trenger test artifacts fra shared:common ---
    testImplementation(project(":shared:common", configuration = "testArtifacts"))

    val exposedVersion = "0.61.0"
    testImplementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
}

tasks.test {
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

kotlin {
    jvmToolchain(21)
}