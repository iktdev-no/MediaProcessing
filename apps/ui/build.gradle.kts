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
    maven { url = uri("https://reposilite.iktdev.no/releases") }
    maven { url = uri("https://reposilite.iktdev.no/snapshots") }
}

dependencies {

    // Spring Boot (BOM styres globalt)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Riktig WebFlux for Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Jackson (BOM-styrt)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Logging (flyttet til root, men beholdes hvis du ikke har lagt det inn der ennå)
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    // Custom libs
    implementation(libs.exfl)
    implementation(project(":shared:common"))
    implementation(project(":transfer-model"))

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
    dependsOn(":transfer-model:generateTs")
    finalizedBy("generateTs")
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
