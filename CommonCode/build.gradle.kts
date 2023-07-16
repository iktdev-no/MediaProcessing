plugins {
    kotlin("jvm") version "1.8.21"
}

group = "no.iktdev.streamit.content"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.pgreze:kotlin-process:1.3.1")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")


    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.assertj:assertj-core:3.4.1")
}

tasks.test {
    useJUnitPlatform()
}