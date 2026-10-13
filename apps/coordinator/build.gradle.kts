import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("org.jetbrains.kotlin.plugin.serialization")
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

    // Spring Boot (BOM styres globalt i root)
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Logging / JSON
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    // Custom libs
    implementation(libs.eventi)

    // Coroutines / utilities

    // Internal modules
    implementation(project(":shared:ffmpeg"))
    implementation(project(":shared:common"))
    implementation(project(":shared:database"))
    implementation(project(":shared:files"))

    // Jackson (versjon styres av Spring Boot BOM)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")




    // --- TESTING ---

    // Spring Boot test stack (inkluderer JUnit 5)
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // AssertJ
    testImplementation("org.assertj:assertj-core:3.21.0")

    // Mockito (kun moderne versjoner)
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    // MockK (moderne versjon)
    testImplementation("io.mockk:mockk:1.13.9")

    // Coroutines test
    testImplementation(libs.kotlinxCoroutinesTest)

    // JSON testing
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    // Test (samme versjon, BOM-styrt)
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Internal test artifacts
    testImplementation(project(":shared:common", configuration = "testArtifacts"))
    testImplementation(project(":shared:database", configuration = "testArtifacts"))
    testImplementation(project(":shared:files", configuration = "testArtifacts"))
    testImplementation(project(":shared:ffmpeg", configuration = "testArtifacts"))


    // Exposed test
    val exposedVersion = "0.61.0"
    testImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val streamitImportOpenApiFile = layout.buildDirectory.file("generated/openapi/openapi-importing.yaml")
val streamitImportOpenApiUrl =
    "https://github.com/iktdev-no/Streamit-service/releases/download/snapshot-9a1bd14b6d5c/openapi-importing.yaml"

tasks.register("downloadStreamitImportOpenApi") {
    group = "streamit"
    description = "Downloads the Streamit importing OpenAPI specification."
    outputs.file(streamitImportOpenApiFile)
    outputs.upToDateWhen { false }

    doLast {
        val outputFile = streamitImportOpenApiFile.get().asFile
        outputFile.parentFile.mkdirs()
        val temporaryFile = outputFile.resolveSibling("${outputFile.name}.download")
        val connection = URL(streamitImportOpenApiUrl).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("User-Agent", "MediaProcessing-OpenAPI-Downloader")
        }

        try {
            if (connection is HttpURLConnection && connection.responseCode !in 200..299) {
                throw GradleException(
                    "Downloading importing OpenAPI spec failed with HTTP ${connection.responseCode} from $streamitImportOpenApiUrl"
                )
            }

            connection.getInputStream().use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!temporaryFile.readText(Charsets.UTF_8).take(4096).contains("openapi:")) {
                throw GradleException("Downloaded file from $streamitImportOpenApiUrl is not an OpenAPI YAML document.")
            }

            Files.move(
                temporaryFile.toPath(),
                outputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            logger.lifecycle("Downloaded Streamit importing OpenAPI spec to $outputFile")
        } finally {
            temporaryFile.delete()
            if (connection is HttpURLConnection) {
                connection.disconnect()
            }
        }
    }
}

tasks.bootJar {
    archiveFileName.set("app.jar")
    launchScript()
}

tasks.jar {
    archiveFileName.set("app.jar")
    archiveBaseName.set("app")
}
