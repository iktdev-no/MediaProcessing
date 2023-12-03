plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "MediaProcessing"
include("apps")
include("shared")
include("shared:kafka")
findProject(":shared:kafka")?.name = "kafka"
include("apps:coordinator")
findProject(":apps:coordinator")?.name = "coordinator"
include("apps:ui")
findProject(":apps:ui")?.name = "ui"
include("apps:encoder")
findProject(":apps:encoder")?.name = "encoder"
include("apps:converter")
findProject(":apps:converter")?.name = "converter"
include("shared:contract")
findProject(":shared:contract")?.name = "contract"
include("shared:common")
findProject(":shared:common")?.name = "common"
include("apps:processer")
findProject(":apps:processer")?.name = "processer"
