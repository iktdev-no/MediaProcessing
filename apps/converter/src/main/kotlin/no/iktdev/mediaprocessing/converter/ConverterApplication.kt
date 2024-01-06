package no.iktdev.mediaprocessing.converter

import kotlinx.coroutines.launch
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class ConvertApplication

private var context: ApplicationContext? = null
@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}
fun main(args: Array<String>) {
    val dataSource = MySqlDataSource.fromDatabaseEnv()
    Coroutines.default().launch {
        dataSource.createDatabase()
        dataSource.createTables(
            processerEvents
        )
    }
    context = runApplication<ConvertApplication>(*args)
}
//private val logger = KotlinLogging.logger {}