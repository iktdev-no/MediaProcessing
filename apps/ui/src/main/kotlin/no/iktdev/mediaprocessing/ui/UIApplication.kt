package no.iktdev.mediaprocessing.ui


import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.exfl.observable.ObservableMap
import no.iktdev.exfl.observable.Observables
import no.iktdev.exfl.observable.observableMapOf
import no.iktdev.mediaprocessing.shared.common.DatabaseEnvConfig
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.toEventsDatabase
import no.iktdev.mediaprocessing.ui.dto.EventDataObject
import no.iktdev.mediaprocessing.ui.dto.ExplorerItem
import no.iktdev.mediaprocessing.ui.dto.SimpleEventDataObject
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}

@SpringBootApplication
class UIApplication {
}

private lateinit var eventsDatabase: MySqlDataSource
fun getEventsDatabase(): MySqlDataSource {
    return eventsDatabase
}

lateinit var persistentReader: PersistentDataReader
lateinit var persistentWriter: PersistentDataStore

private var context: ApplicationContext? = null
private val kafkaClearedLatch = CountDownLatch(1)

@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}

val memSimpleConvertedEventsMap: ObservableMap<String, SimpleEventDataObject> = observableMapOf()
val memActiveEventMap: ObservableMap<String, EventDataObject> = observableMapOf()
val fileRegister: ObservableMap<String, ExplorerItem> = observableMapOf()

fun main(args: Array<String>) {

    eventsDatabase = DatabaseEnvConfig.toEventsDatabase()
    eventsDatabase.connect()

    persistentReader = PersistentDataReader(eventsDatabase)
    persistentWriter = PersistentDataStore(eventsDatabase)


    Coroutines.addListener(object : Observables.ObservableValue.ValueListener<Throwable> {
        override fun onUpdated(value: Throwable) {
            logger.error { "Received error: ${value.message}" }
            value.cause?.printStackTrace()
        }
    })

    try {
        /*val admincli = AdminClient.create(mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaEnv.servers,
            AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG to "1000",
            AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG to "5000"
        ))
        val go = admincli.listConsumerGroupOffsets("${KafkaEnv.consumerId}:UIDataComposer")
        go.partitionsToOffsetAndMetadata().whenComplete { result, throwable ->
            val partitions = result.entries.filter { it.key.topic() == SharedConfig.kafkaTopic }
                .map { it.key }
            val deleteResult = admincli.deleteConsumerGroupOffsets("${KafkaEnv.consumerId}:UIDataComposer", partitions.toSet())
            deleteResult.all().whenComplete { result, throwable ->
                kafkaClearedLatch.countDown()
            }
        }*/

    } catch (e: Exception) {
        e.printStackTrace()
      //  kafkaClearedLatch.countDown()
    }

 //   logger.info { "Waiting for kafka to clear offset!" }
   // kafkaClearedLatch.await(5, TimeUnit.MINUTES)
 //   logger.info { "Offset cleared!" }
  //  Thread.sleep(10000)
    context = runApplication<UIApplication>(*args)

}




