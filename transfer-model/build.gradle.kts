import no.iktdev.ts.TsGenerator
import org.gradle.api.tasks.SourceSetContainer
import java.net.URLClassLoader


plugins {
    kotlin("jvm")
}

group = "no.iktdev.mediaprocessing"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
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



kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}