plugins {
    id("java")
    kotlin("jvm")
}

group = "no.iktdev.mediaprocessing.shared"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven { url = uri("https://reposilite.iktdev.no/releases") }
    maven { url = uri("https://reposilite.iktdev.no/snapshots") }
}
