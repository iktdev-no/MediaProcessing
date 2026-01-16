plugins {
    id("java")
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "no.iktdev.mediaprocessing.apps"
version = "1.0-SNAPSHOT"

val appVersion= "1.0.0"

tasks.processResources {
    expand(mapOf("appVersion" to appVersion))
}


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
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-tx")



    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    implementation(libs.exfl)
    implementation("no.iktdev.library:subtitle:1.8.1-SNAPSHOT")
    implementation(libs.eventi)


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
    implementation("com.github.pgreze:kotlin-process:1.4.1")

    implementation(project(mapOf("path" to ":shared:common")))
    implementation(project(mapOf("path" to ":shared:database")))

    implementation(kotlin("stdlib-jdk8"))

    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":shared:common", configuration = "testArtifacts"))
    testImplementation(project(":shared:database", configuration = "testArtifacts"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

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