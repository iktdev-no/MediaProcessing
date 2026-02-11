package no.iktdev.ts

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

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
                    // ----------------------------------------------------
                    // 1) Sealed interface → union type alias
                    // ----------------------------------------------------
                    cls.isSealed && cls.java.isInterface -> {
                        val subTypes = cls.sealedSubclasses
                            .mapNotNull { it.simpleName }
                            .joinToString(" | ")

                        println("Generating union type for sealed interface: ${cls.simpleName} = $subTypes")
                        appendLine("export type ${cls.simpleName} = $subTypes")
                    }

                    // 1a) Sealed class → union type alias
                    cls.isSealed && !cls.java.isInterface -> {
                        val subTypes = cls.sealedSubclasses
                            .mapNotNull { it.simpleName }
                            .joinToString(" | ")

                        println("Generating union type for sealed class: ${cls.simpleName} = $subTypes")
                        appendLine("export type ${cls.simpleName} = $subTypes")
                    }


                    // ----------------------------------------------------
                    // 1b) Sealed subtype (data object / data class)
                    //     → generer interface selv uten properties
                    // ----------------------------------------------------
                    cls.isSealedSubtype() -> {
                        println("Generating sealed subtype interface: ${cls.simpleName}")
                        append(sealedSubtypeToTs(cls))
                    }

                    // ----------------------------------------------------
                    // 2) Enum → union of string literals
                    // ----------------------------------------------------
                    cls.java.isEnum -> {
                        println("Generating enum: ${cls.simpleName}")
                        append(enumToTs(cls))
                    }

                    // ----------------------------------------------------
                    // 3) Data class → interface
                    // ----------------------------------------------------
                    cls.hasProperties() && !cls.isSealed -> {
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

    // sealed subtype = har en sealed supertype
    private fun KClass<*>.isSealedSubtype(): Boolean =
        this.supertypes
            .mapNotNull { it.classifier as? KClass<*> }
            .any { it.isSealed }

    // ------------------------------------------------------------
    // Kotlin → TS mapping
    // ------------------------------------------------------------

    private fun enumToTs(cls: KClass<*>): String {
        val values = cls.java.enumConstants
            .joinToString(" | ") { "\"$it\"" }

        return "export type ${cls.simpleName} = $values\n"
    }

    private fun sealedSubtypeToTs(cls: KClass<*>): String {
        val props = cls.memberProperties
            .filter { it.name != "type" } // ← FIX: unngå duplisering
            .joinToString("\n") { prop ->
                val tsType = kotlinToTsType(prop.returnType.toString())
                "  ${prop.name}: $tsType;"
            }

        val propsBlock = if (props.isBlank()) "" else "\n$props"

        return buildString {
            appendLine("export interface ${cls.simpleName} {")
            appendLine("  type: \"${cls.simpleName}\";$propsBlock")
            appendLine("}")
        }
    }


    // NEW: extract default value for "type" discriminator
    private fun getDefaultTypeLiteral(cls: KClass<*>): String? {
        val ctor = cls.primaryConstructor ?: return null
        val param = ctor.parameters.find { it.name == "type" } ?: return null

        return try {
            val instance = ctor.callBy(emptyMap())
            val prop = cls.memberProperties.find { it.name == "type" }
            prop?.getter?.call(instance)?.toString()
        } catch (_: Throwable) {
            null
        }
    }

    private fun dataClassToTs(cls: KClass<*>): String {
        val typeParams = cls.typeParameters.map { it.name }
        val generic = if (typeParams.isNotEmpty()) {
            "<" + typeParams.joinToString(", ") + ">"
        } else ""

        val defaultTypeLiteral = getDefaultTypeLiteral(cls)

        val props = cls.memberProperties.joinToString("\n") { prop ->
            val tsType =
                if (prop.name == "type" && defaultTypeLiteral != null) {
                    "\"$defaultTypeLiteral\"" // literal type
                } else {
                    kotlinToTsType(prop.returnType.toString(), typeParams)
                }

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

        // Set<T> → T[]
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

        // List<T> → T[]
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
