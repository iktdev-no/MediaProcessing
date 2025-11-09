plugins {
    kotlin("jvm")
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


    implementation("com.github.pgreze:kotlin-process:1.5.1")
    implementation("com.google.code.gson:gson:2.8.9")

    implementation("no.iktdev:exfl:1.0-rc1")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}