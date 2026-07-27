import no.iktdev.ts.TsGenerator
import java.net.URLClassLoader
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec

plugins {
    id("java")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("com.github.node-gradle.node") version "7.0.2"
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
    implementation("io.projectreactor.netty:reactor-netty")
    implementation("io.projectreactor.netty:reactor-netty-http")


    // Jackson (BOM-styrt)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Logging (flyttet til root, men beholdes hvis du ikke har lagt det inn der ennå)
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")

    // Custom libs
    implementation(libs.exfl)
    implementation(project(":shared:common"))
    implementation(project(":transfer-model"))
    implementation(project(":shared:files"))


    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":shared:files", configuration = "testArtifacts"))

}

tsGenerator {
    packageName.set("no.iktdev.mediaprocessing.ui.dto")
    outputFile.set(file("$projectDir/web/src/types/types.d.ts"))
}

/*tasks.register("generateTs") {
    doLast {
        val classesDir = file("$projectDir/build/classes/kotlin/main")
        val cl = URLClassLoader(arrayOf(classesDir.toURI().toURL()), TsGenerator::class.java.classLoader)

        TsGenerator.generate(
            packageName = "no.iktdev.mediaprocessing.ui.dto",
            output = file("$projectDir/web/src/types/types.ts"),
            classLoader = cl
        )
    }
}*/

tasks.named("build") {
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

// --- React build tasks (using system npm) ---

tasks.register<Exec>("npmInstallWeb") {
    workingDir = file("$projectDir/web")
    commandLine("npm", "install")
}

tasks.register<Exec>("npmBuildWeb") {
    dependsOn("npmInstallWeb")
    workingDir = file("$projectDir/web")
    commandLine("npm", "run", "build")
}

tasks.register<Copy>("copyWebToStatic") {
    dependsOn("npmBuildWeb")
    from("$projectDir/web/dist")
    into("$projectDir/src/main/resources/static")
}

// Ensure Spring Boot includes the built frontend
tasks.named("processResources") {
    dependsOn("copyWebToStatic")
}
