package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents
import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

private val logger = KotlinLogging.logger {}

@SpringBootApplication
class ProcesserApplication {
}

fun main(args: Array<String>) {
    val dataSource = MySqlDataSource.fromDatabaseEnv()
    Coroutines.default().launch {
        dataSource.createDatabase()
        dataSource.createTables(
            processerEvents
        )
    }
    val context = runApplication<ProcesserApplication>(*args)
}

class SocketImplemented: SocketImplementation() {

}