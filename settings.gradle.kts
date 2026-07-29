pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            url = uri("https://reposilite.iktdev.no/releases")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "MediaProcessing"

findProject(":apps:ui")?.name = "ui"
findProject(":apps:coordinator")?.name = "coordinator"
findProject(":apps:converter")?.name = "converter"
findProject(":apps:processer")?.name = "processer"


findProject(":shared")?.name = "shared"
findProject(":shared:ffmpeg")?.name = "ffmpeg"
findProject(":shared:common")?.name = "common"
findProject(":shared:files")?.name = "files"


include("apps")
include("apps:ui")
include("apps:coordinator")
include("apps:converter")
include("apps:processer")

include("shared")
include("shared:common")

include("shared:ffmpeg")
include("shared:database")
include("shared:database")
include("shared:files")
