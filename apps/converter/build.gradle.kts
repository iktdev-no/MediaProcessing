plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

group = "no.iktdev.mediaprocessing.apps"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven { url = uri("https://reposilite.iktdev.no/releases") }
    maven { url = uri("https://reposilite.iktdev.no/snapshots") }
}

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework:spring-tx")

    // Jackson (BOM-styrt)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Custom libs
    implementation(libs.exfl)
    implementation("no.iktdev.library:subtitle:1.8.1-SNAPSHOT")
    implementation(libs.eventi)

    // Coroutines / utilities
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.github.pgreze:kotlin-process:1.4.1")

    // Internal modules
    implementation(project(":shared:common"))
    implementation(project(":shared:database"))
    implementation(project(":shared:files"))


    // --- TESTING ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation(project(":shared:common", configuration = "testArtifacts"))
    testImplementation(project(":shared:database", configuration = "testArtifacts"))
    testImplementation(project(":shared:files", configuration = "testArtifacts"))


    val exposedVersion = "0.61.0"
    testImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
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
