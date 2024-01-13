plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "shared"

findProject(":shared:kafka")?.name = "kafka"
findProject(":shared:contract")?.name = "contract"
findProject(":shared:common")?.name = "common"

include("kafka")
include("contract")
include("common")


