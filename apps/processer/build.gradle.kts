plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring") version "1.5.31"
    id("org.springframework.boot") version "2.5.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
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

val exposedVersion = "0.44.0"
dependencies {

    /*Spring boot*/
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter:2.7.0")
   // implementation("org.springframework.kafka:spring-kafka:3.0.1")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")
    implementation("org.springframework.kafka:spring-kafka:2.8.5")


    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation ("mysql:mysql-connector-java:8.0.29")

    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    implementation("no.iktdev:exfl:0.0.13-SNAPSHOT")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")
    implementation("com.github.pgreze:kotlin-process:1.4.1")

    //implementation(project(mapOf("path" to ":shared")))
    implementation(project(mapOf("path" to ":shared:contract")))
    implementation(project(mapOf("path" to ":shared:common")))
    implementation(project(mapOf("path" to ":shared:kafka")))


    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.assertj:assertj-core:3.4.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("io.kotlintest:kotlintest-assertions:3.3.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation(kotlin("stdlib-jdk8"))
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