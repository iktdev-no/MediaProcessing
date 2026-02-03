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
    maven {
        url = uri("https://reposilite.iktdev.no/releases")
    }
    maven {
        url = uri("https://reposilite.iktdev.no/snapshots")
    }
}


dependencies {

    /*Spring boot*/
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-validation")


    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.json:json:20210307")

    implementation(libs.exfl)
    implementation("no.iktdev.streamit.library:streamit-library-db:1.0.0-alpha14")
    implementation(libs.eventi)


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("com.github.vishna:watchservice-ktx:master-SNAPSHOT")

    implementation(project(":transfer-model"))

    implementation(project(mapOf("path" to ":shared:ffmpeg")))
    implementation(project(mapOf("path" to ":shared:common")))
    implementation(project(mapOf("path" to ":shared:database")))



    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.assertj:assertj-core:3.21.0")


    testImplementation("junit:junit:4.12")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")

    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    testImplementation("org.mockito:mockito-core:3.+")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.assertj:assertj-core:3.4.1")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.assertj:assertj-core:3.4.1")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.assertj:assertj-core:3.4.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation(project(":shared:common", configuration = "testArtifacts"))
    testImplementation(project(":shared:database", configuration = "testArtifacts"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    val exposedVersion = "0.61.0"
    testImplementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")


}

tasks.withType<Test> {
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