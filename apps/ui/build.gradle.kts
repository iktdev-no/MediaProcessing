import no.iktdev.ts.TsGenerator
import java.net.URLClassLoader

plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
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

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Spring Boot (WebFlux gir deg SSE + non-blocking IO)
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework:spring-webflux")

    // JSON (Jackson Kotlin)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")


    // Dine custom libs
    implementation(libs.exfl)
    implementation(project(":shared:common"))
    implementation(project(":transfer-model"))

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.register("generateTs") {
    doLast {
        val classesDir = file("$projectDir/build/classes/kotlin/main")
        val cl = URLClassLoader(arrayOf(classesDir.toURI().toURL()), TsGenerator::class.java.classLoader)

        TsGenerator.generate(
            packageName = "no.iktdev.mediaprocessing.ui.dto",
            output = file("$projectDir/web/src/types/types.d.ts"),
            classLoader = cl
        )
    }
}

tasks.named("build") {
    finalizedBy("generateTs")
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.bootJar {
    archiveFileName.set("app.jar")
    launchScript()
}

tasks.jar {
    archiveFileName.set("app.jar")
    archiveBaseName.set("app")
}