plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "MediaProcessing"

findProject(":apps:ui")?.name = "ui"
findProject(":apps:coordinator")?.name = "coordinator"
findProject(":apps:converter")?.name = "converter"
findProject(":apps:processer")?.name = "processer"


findProject(":shared")?.name = "shared"
findProject(":shared:contract")?.name = "contract"
findProject(":shared:common")?.name = "common"
findProject(":shared:eventi")?.name = "eventi"

include("apps")
include("apps:ui")
include("apps:coordinator")
include("apps:converter")
include("apps:processer")

include("shared")
include("shared:contract")
include("shared:common")
include("shared:eventi")
