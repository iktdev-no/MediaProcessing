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
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    implementation(project(mapOf("path" to ":shared:contract")))



    implementation("org.springframework.kafka:spring-kafka:2.8.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.classgraph:classgraph:4.8.165")

    testImplementation("org.springframework.kafka:spring-kafka-test:3.0.1")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.assertj:assertj-core:3.4.1")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.assertj:assertj-core:3.4.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.mockito:mockito-core:3.10.0") // Oppdater versjonen hvis det er nyere tilgjengelig
    testImplementation("org.mockito:mockito-junit-jupiter:3.10.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
