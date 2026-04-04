import java.util.regex.Matcher

plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

group = "no.iktdev.mediaprocessing.apps"
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
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework:spring-tx")

    // Logging / JSON
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    // Jackson aligned
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Coroutines / utilities
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("no.iktdev:process-runner:1.0.0")

    // Custom libs
    implementation(libs.exfl)
    implementation(libs.eventi)

    // Internal modules
    implementation(project(":shared:common"))
    implementation(project(":shared:database"))
    implementation(project(":shared:ffmpeg"))
    implementation(project(":transfer-model"))
    implementation(project(":shared:files"))


    // --- TESTING ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.assertj:assertj-core:3.24.2")

    // Test artifacts
    testImplementation(project(":shared:common", configuration = "testArtifacts"))
    testImplementation(project(":shared:database", configuration = "testArtifacts"))
    testImplementation(project(":shared:files", configuration = "testArtifacts"))

    val exposedVersion = "0.61.0"
    testImplementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xint")
    jvmArgs("-Dkotlinx.coroutines.debug")
    jvmArgs("-Dkotlinx.coroutines.scheduler.corePoolSize=1")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
    launchScript()
}

tasks.jar {
    archiveFileName.set("app.jar")
    archiveBaseName.set("app")
}

tasks.register("syncGovenorScript") {
    group = "documentation"
    description = "Embeds governor.sh into all README*.md files between placeholders."

    val script = project.file("governor.sh")

    doLast {
        if (!script.exists()) error("governor.sh not found")

        val scriptText = script.readText()
        val startTag = "<!-- GOVENOR_SH_START -->"
        val endTag = "<!-- GOVENOR_SH_END -->"

        val readmes = project.projectDir
            .listFiles { file -> file.name.matches(Regex("README(\\.[A-Za-z]+)?\\.md")) }
            ?.toList()
            ?: emptyList()

        if (readmes.isEmpty()) {
            println("No README*.md files found")
            return@doLast
        }

        readmes.forEach { readme ->
            val readmeText = readme.readText()

            if (!readmeText.contains(startTag) || !readmeText.contains(endTag)) {
                println("Skipping ${readme.name}: missing placeholders")
                return@forEach
            }

            // No indent, no trimIndent, no whitespace pollution
            val replacement = buildString {
                append(startTag).append("\n")
                append("```bash\n")
                append(scriptText)
                append("\n```\n")
                append(endTag)
            }

            val safeReplacement = Matcher.quoteReplacement(replacement)

            val newContent = readmeText.replace(
                Regex("$startTag[\\s\\S]*?$endTag"),
                safeReplacement
            )

            readme.writeText(newContent)
            println("Updated ${readme.name}")
        }
    }
}
