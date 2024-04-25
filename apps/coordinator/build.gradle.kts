plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring") version "1.5.31"
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.0" // Legg til Kotlin Serialization-plugin
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

    /*Spring boot*/
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter:3.2.0")
    // implementation("org.springframework.kafka:spring-kafka:3.0.1")
    implementation("org.springframework.kafka:spring-kafka:2.8.5")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")

    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    implementation("no.iktdev:exfl:0.0.16-SNAPSHOT")
    implementation("no.iktdev.streamit.library:streamit-library-db:0.0.6-alpha27")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")

    //implementation(project(mapOf("path" to ":shared")))
    implementation(project(mapOf("path" to ":shared:kafka")))

    implementation(project(mapOf("path" to ":shared:contract")))
    implementation(project(mapOf("path" to ":shared:common")))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation ("mysql:mysql-connector-java:8.0.29")



    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.assertj:assertj-core:3.21.0")

    testImplementation("junit:junit:4.12")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    testImplementation("org.mockito:mockito-core:3.+")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.assertj:assertj-core:3.4.1")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.assertj:assertj-core:3.4.1")

    /*testImplementation("org.junit.vintage:junit-vintage-engine")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0") // Oppdater versjonen hvis det er nyere tilgjengelig
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.platform:junit-platform-runner:1.10.1")*/

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.assertj:assertj-core:3.4.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.bootJar {
    archiveFileName.set("app.jar")
    launchScript()
}

tasks.jar {
    archiveFileName.set("app.jar")
    archiveBaseName.set("app")
}