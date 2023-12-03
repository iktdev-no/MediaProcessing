package no.iktdev.mediaprocessing.shared.kafka.core

import java.io.File
import kotlin.reflect.KClass

class AnnotationFinder {
    fun getClassesWithAnnotation(packageName: String, annotation: KClass<out Annotation>): List<KClass<*>> {
        val packageToScan = packageName.replace('.', '/')
        val classLoader = Thread.currentThread().contextClassLoader
        val resources = classLoader.getResources(packageToScan)

        val classes = mutableListOf<KClass<*>>()

        while (resources.hasMoreElements()) {
            val resource = resources.nextElement()
            if (resource.protocol == "file") {
                val file = File(resource.file)
                if (file.isDirectory) {
                    val classNames = file.walkTopDown().filter { it.isFile && it.extension == "class" }
                        .map { it.toRelativeString(file).removeSuffix(".class").replace('/', '.') }
                        .toList()

                    classNames.forEach { className ->
                        try {
                            val loadedClass = Class.forName(className).kotlin
                            if (loadedClass.annotations.any { it.annotationClass == annotation }) {
                                classes.add(loadedClass)
                            }
                        } catch (e: ClassNotFoundException) {
                            // Handle exception if needed
                        }
                    }
                }
            }
        }

        return classes
    }
}