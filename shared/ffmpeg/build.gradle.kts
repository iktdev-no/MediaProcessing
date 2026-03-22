plugins {
    kotlin("jvm")
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
    implementation("com.github.pgreze:kotlin-process:1.5.1")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(libs.exfl)

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.assertj:assertj-core:3.4.1")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
    implementation(project(":shared:files"))

    testImplementation(project(":shared:files", configuration = "testArtifacts"))


}

tasks.test {
    useJUnitPlatform()
}

configurations { create("testArtifacts") }

tasks.register<Jar>("testJar") {
    dependsOn("testClasses")
    from(sourceSets.test.get().output)
    archiveClassifier.set("tests")
}

artifacts {
    add("testArtifacts", tasks.named("testJar"))
}
