package no.iktdev.streamit.content.encode

import org.apache.kafka.clients.consumer.ConsumerRecord

open class Resources {

    fun getText(path: String): String? {
        return this.javaClass.classLoader.getResource(path)?.readText()
    }

    open class Streams(): Resources() {
        fun all(): List<String> {
            return listOf<String>(
                getSample(0),
                getSample(1),
                getSample(2),
                getSample(3),
                getSample(4),
                getSample(5),
                getSample(6),
            )
        }

        fun getSample(number: Int): String {
            return getText("streams/sample$number.json")!!
        }
    }

}