import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.8.21"

    kotlin("plugin.spring") version "1.6.21"
}

base.archivesBaseName = "ui"

group = "no.iktdev.streamit.content"
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
    implementation("no.iktdev.streamit.library:streamit-library-kafka:0.0.2-alpha85")

    implementation("org.springframework.boot:spring-boot-starter-web:3.0.4")
    implementation("org.springframework.kafka:spring-kafka:2.8.5")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.springframework.boot:spring-boot-starter-websocket:2.6.3")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")


    implementation("no.iktdev:exfl:0.0.13-SNAPSHOT")




    implementation(project(":CommonCode"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    dependsOn(":buildFrontend")
}


tasks.register<Exec>("buildFrontend") {
    workingDir = file("web") // Stien til frontend-mappen
    commandLine("npm", "install") // Installer frontend-avhengigheter
    commandLine("npm", "run", "build") // Bygg frontend

    doLast {
        copy {
            from(file("web/build")) // Byggresultatet fra React-appen
            into(file("src/main/resources/static/")) // Mappen der du vil plassere det i Spring Boot-prosjektet
        }
    }
}

// Kjør frontendbygget før backendbygget
//tasks.getByName("bootJar").dependsOn("buildFrontend")
