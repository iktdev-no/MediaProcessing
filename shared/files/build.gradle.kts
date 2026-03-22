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
