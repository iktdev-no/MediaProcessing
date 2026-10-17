tasks.named("fabriktGenerate") {
    doLast {
        val generatedSources = file("src/main/generated/kotlin")
        generatedSources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { source ->
                val lines = source.readLines().toMutableList()
                var index = 0
                while (index + 2 < lines.size) {
                    val parameterAnnotation = Regex("""^(\s*)@param:JsonProperty\("([^"]+)"\)$""")
                        .matchEntire(lines[index])
                    val getterAnnotation = Regex("""^(\s*)@get:JsonProperty\("([^"]+)"\)$""")
                        .matchEntire(lines[index + 1])
                    val property = Regex("""^(\s*)public val (`?)([A-Za-z_][A-Za-z0-9_]*)(`?):.*$""")
                        .matchEntire(lines[index + 2])

                    if (parameterAnnotation != null && getterAnnotation != null && property != null) {
                        val jsonName = parameterAnnotation.groupValues[2]
                        val propertyName = property.groupValues[3]
                        if (jsonName == propertyName &&
                            parameterAnnotation.groupValues[1] == getterAnnotation.groupValues[1] &&
                            jsonName == getterAnnotation.groupValues[2]
                        ) {
                            lines.removeAt(index + 1)
                            lines.removeAt(index)
                            continue
                        }
                    }
                    index++
                }

                var normalized = lines.joinToString("\n")
                    .replace(Regex("\\bpublic (data class|enum class|val|fun|companion object)\\b"), "${'$'}1")
                if (!normalized.contains("JsonProperty(")) {
                    normalized = normalized.replace(
                        Regex("(?m)^import com\\.fasterxml\\.jackson\\.`annotation`\\.JsonProperty\\n"),
                        ""
                    )
                }
                source.writeText(normalized + "\n")
            }
    }
}
