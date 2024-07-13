plugins {
    id("java")
    kotlin("jvm")
}

group = "no.iktdev.mediaprocessing.shared"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation("com.google.code.gson:gson:2.8.9")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    implementation("org.springframework.boot:spring-boot-starter:2.7.0")
    implementation(project(mapOf("path" to ":shared:eventi")))


    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}