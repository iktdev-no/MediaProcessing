package no.iktdev.ts

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

object TsGenerator {

    fun generate(packageName: String, output: File, classLoader: ClassLoader = Thread.currentThread().contextClassLoader) {
        println("TsGenerator: scanning package: $packageName")

        val classes = scanClasses(packageName, classLoader)

        println("TsGenerator: found ${classes.size} classes total")
        classes.forEach { println(" - ${it.qualifiedName}") }

        val ts = buildString {
            appendLine("// AUTO-GENERATED. DO NOT EDIT.")
            appendLine("// Source: $packageName")
            appendLine()

            classes.forEach { cls ->
                when {
                    cls.java.isEnum -> {
                        println("Generating enum: ${cls.simpleName}")
                        append(enumToTs(cls))
                    }

                    cls.hasProperties() -> {
                        println("Generating interface: ${cls.simpleName}")
                        append(dataClassToTs(cls))
                    }

                    else -> {
                        println("IGNORED (no properties): ${cls.qualifiedName}")
                    }
                }
                appendLine()
            }
        }

        output.parentFile.mkdirs()
        output.writeText(ts)

        println("TsGenerator: wrote file to ${output.absolutePath}")
    }

    private fun File.toClassName(rootPath: String): String {
        val full = this.path.replace(File.separatorChar, '/')
        val idx = full.indexOf(rootPath)
        val relative = full.substring(idx)
        return relative
            .removeSuffix(".class")
            .replace('/', '.')
    }

    private fun KClass<*>.hasProperties(): Boolean =
        this.memberProperties.isNotEmpty()

    // ------------------------------------------------------------
    // Kotlin → TS mapping
    // ------------------------------------------------------------

    private fun enumToTs(cls: KClass<*>): String {
        val values = cls.java.enumConstants
            .joinToString(" | ") { "\"$it\"" }

        return "export type ${cls.simpleName} = $values\n"
    }

    private fun dataClassToTs(cls: KClass<*>): String {
        val typeParams = cls.typeParameters.map { it.name } // generics på klassen
        val generic = if (typeParams.isNotEmpty()) {
            "<" + typeParams.joinToString(", ") + ">"
        } else ""

        val props = cls.memberProperties.joinToString("\n") { prop ->
            val tsType = kotlinToTsType(prop.returnType.toString(), typeParams)
            "  ${prop.name}: $tsType;"
        }

        return buildString {
            appendLine("export interface ${cls.simpleName}$generic {")
            appendLine(props)
            appendLine("}")
        }
    }

    // ------------------------------------------------------------
    // Type mapping
    // ------------------------------------------------------------

    private fun kotlinToTsType(kotlinType: String, genericParams: List<String> = emptyList()): String {
        val isNullable = kotlinType.endsWith("?")
        val clean = kotlinType.removeSuffix("?")

        // UUID → string
        if (clean == "java.util.UUID") {
            return if (isNullable) "string | null" else "string"
        }

        // Instant → string
        if (clean == "java.time.Instant") {
            return if (isNullable) "string | null" else "string"
        }

        // Duration → string
        if (clean in listOf("kotlin.time.Duration", "java.time.Duration")) {
            return if (isNullable) "string | null" else "string"
        }

        // Map<K, V> → Record<K, V>
        if (clean.startsWith("kotlin.collections.Map") ||
            clean.startsWith("kotlin.collections.MutableMap")) {

            val inner = clean.substringAfter("<").substringBeforeLast(">")
            val (key, value) = inner.split(",").map { it.trim() }

            val tsKey = kotlinToTsType(key, genericParams)
            val tsValue = kotlinToTsType(value, genericParams)

            val base = "Record<$tsKey, $tsValue>"
            return if (isNullable) "$base | null" else base
        }

        // Set<T> → T[] (generics-aware)
        if (clean.startsWith("kotlin.collections.Set") ||
            clean.startsWith("kotlin.collections.MutableSet")) {

            val inner = clean.substringAfter("<").substringBeforeLast(">")

            if (genericParams.contains(inner)) {
                val base = "${inner}[]"
                return if (isNullable) "$base | null" else base
            }

            val tsInner = kotlinToTsType(inner, genericParams)
            val base = "$tsInner[]"
            return if (isNullable) "$base | null" else base
        }

        // List<T> → T[] (generics-aware)
        if (clean.startsWith("kotlin.collections.List") ||
            clean.startsWith("kotlin.collections.MutableList")) {

            val inner = clean.substringAfter("<").substringBeforeLast(">")

            if (genericParams.contains(inner)) {
                val base = "${inner}[]"
                return if (isNullable) "$base | null" else base
            }

            val tsInner = kotlinToTsType(inner, genericParams)
            val base = "$tsInner[]"
            return if (isNullable) "$base | null" else base
        }

        // Primitive mappings
        val base = when (clean) {
            "kotlin.String" -> "string"
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Float",
            "kotlin.Double" -> "number"
            "kotlin.Boolean" -> "boolean"
            "kotlin.Any" -> "any"
            else -> {
                if (clean.contains(".")) clean.substringAfterLast(".")
                else "any"
            }
        }

        return if (isNullable) "$base | null" else base
    }

    // ------------------------------------------------------------
    // Class scanning
    // ------------------------------------------------------------

    private fun scanClasses(packageName: String, classLoader: ClassLoader): List<KClass<*>> {
        val path = packageName.replace('.', '/')
        println("TsGenerator: looking for resources at path: $path")

        val resources = classLoader.getResources(path)

        val classes = mutableListOf<KClass<*>>()

        if (!resources.hasMoreElements()) {
            println("TsGenerator: NO RESOURCES FOUND for path: $path")
        }

        while (resources.hasMoreElements()) {
            val url = resources.nextElement()
            println("TsGenerator: resource URL = $url")

            val file = File(url.toURI())
            println("TsGenerator: resolved file = ${file.absolutePath}, exists=${file.exists()}, isDir=${file.isDirectory}")

            if (file.exists() && file.isDirectory) {
                file.walkTopDown().forEach { f ->
                    if (f.extension == "class") {
                        val className = f.toClassName(path)
                        try {
                            val cls = Class.forName(className, false, classLoader).kotlin
                            classes.add(cls)
                        } catch (_: Throwable) {}
                    }
                }
            }
        }

        return classes
    }
}
