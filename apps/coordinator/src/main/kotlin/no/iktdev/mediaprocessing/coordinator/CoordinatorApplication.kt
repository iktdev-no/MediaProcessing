package no.iktdev.mediaprocessing.coordinator

import kotlinx.coroutines.launch
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.datasource.MySqlDataSource
import no.iktdev.mediaprocessing.shared.persistance.events
import no.iktdev.mediaprocessing.shared.socket.SocketImplementation
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class CoordinatorApplication {
}

fun main(args: Array<String>) {
    val dataSource = MySqlDataSource.fromDatabaseEnv();
    dataSource.createDatabase()
    dataSource.createTables(
        events
    )
}


class SocketImplemented: SocketImplementation() {

}
