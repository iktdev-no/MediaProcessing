package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.common.persistance.processerEvents
import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

private val logger = KotlinLogging.logger {}

@SpringBootApplication
class ProcesserApplication {
}
lateinit var dataSource: MySqlDataSource
fun main(args: Array<String>) {
    dataSource = MySqlDataSource.fromDatabaseEnv()
    dataSource.createDatabase()
    dataSource.createTables(
        processerEvents
    )
    val context = runApplication<ProcesserApplication>(*args)
}

class SocketImplemented: SocketImplementation() {

}
@EnableScheduling
class DatabaseReconnect() {
    var lostConnectionCount = 0
    @Scheduled(fixedDelay = (100_000))
    fun checkIfConnected() {
        if (TransactionManager.currentOrNull() == null) {
            lostConnectionCount++
            dataSource.toDatabase()
        }
    }
}