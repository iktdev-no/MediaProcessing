package no.iktdev.mediaprocessing.shared.common.database.cal

import mu.KotlinLogging
import no.iktdev.eventi.database.DataSource
import no.iktdev.eventi.database.executeOrException
import no.iktdev.eventi.database.withDirtyRead
import no.iktdev.mediaprocessing.shared.common.database.tables.runners
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.UUID

class RunnerManager(private val dataSource: DataSource, val startId: String = UUID.randomUUID().toString(), val applicationName: String) {
    private val log = KotlinLogging.logger {}

    fun assignRunner(): Boolean {
        return executeOrException(dataSource.database) {
            runners.insert {
                it[runners.startId] = this@RunnerManager.startId
                it[runners.application] = this@RunnerManager.applicationName
                it[runners.version] = getAppVersion()
            }
        } == null
    }

    fun amIEnabled(): Boolean {
        return withDirtyRead(dataSource.database) {
            runners.select {
                (runners.application eq applicationName) and
                        (runners.startId eq startId)
            }.singleOrNull()?.get(runners.enabled)
        } ?: run {
            log.error { "Failed to get a response, reporting false for enabled" }
            false
        }
    }

    fun iAmSuperseded(): Boolean {
        return withDirtyRead(dataSource.database) {
            val runnerVersionCodes = runners.select {
                (runners.application eq this@RunnerManager.applicationName) and
                (runners.startId neq this@RunnerManager.startId)

            }.map { it[runners.version] }

            val myVersion = getAppVersion()
            myVersion.let {
                (runnerVersionCodes.any { rv -> rv > it })
            } ?: true
        } ?: true
    }
}

enum class ActiveMode {
    Active,
    Passive
}