import no.iktdev.ts.TsGenerator
import java.net.URLClassLoader

plugins {
    kotlin("jvm")
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
    // Kotlin test
    testImplementation(kotlin("test"))

    // Reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Jackson (BOM-styrt fra root)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Logging, gson, org.json, coroutines → kommer fra root
}

tasks.register("generateTs") {
    doLast {
        val classesDir = file("$projectDir/build/classes/kotlin/main")
        val cl = URLClassLoader(arrayOf(classesDir.toURI().toURL()), TsGenerator::class.java.classLoader)

        TsGenerator.generate(
            packageName = "no.iktdev.mediaprocessing.transferModel.coordinatorUi",
            output = file("../apps/ui/web/src/types/transfer-model.d.ts"),
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
