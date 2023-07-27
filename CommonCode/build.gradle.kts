plugins {
    kotlin("jvm") version "1.8.21"
}

group = "no.iktdev.streamit.content"
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
    implementation("com.github.pgreze:kotlin-process:1.3.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    implementation("no.iktdev.streamit.library:streamit-library-kafka:0.0.2-alpha76")
    implementation("no.iktdev:exfl:0.0.12-SNAPSHOT")

    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20230227")



    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.assertj:assertj-core:3.4.1")
}

tasks.test {
    useJUnitPlatform()
}