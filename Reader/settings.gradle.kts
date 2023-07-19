rootProject.name = "Reader"

include(":CommonCode")
project(":CommonCode").projectDir = File("../CommonCode")

include(":streamit-library-kafka")