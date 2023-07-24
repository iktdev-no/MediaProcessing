package no.iktdev.streamit.content.reader

import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.streamit.content.reader.analyzer.encoding.helpers.PreferenceReader
import no.iktdev.streamit.library.db.datasource.MySqlDataSource
import no.iktdev.streamit.library.db.tables.*
import no.iktdev.streamit.library.db.tables.helper.cast_errors
import no.iktdev.streamit.library.db.tables.helper.data_audio
import no.iktdev.streamit.library.db.tables.helper.data_video
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

private val logger = KotlinLogging.logger {}

@SpringBootApplication
class ReaderApplication

val preference = PreferenceReader().getPreference()
private var context: ApplicationContext? = null

@Suppress("unused")
fun getContext(): ApplicationContext? {
    return context
}
fun main(args: Array<String>) {

    val ds = MySqlDataSource.fromDatabaseEnv().createDatabase()
    System.out.println(ds)

    Coroutines.default().launch {
        val tables = arrayOf(
            catalog,
            genre,
            movie,
            serie,
            subtitle,
            summary,
            users,
            progress,
            data_audio,
            data_video,
            cast_errors
        )
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*tables)
            logger.info {"Database transaction completed"}
        }
    }

    context = runApplication<ReaderApplication>(*args)

}

