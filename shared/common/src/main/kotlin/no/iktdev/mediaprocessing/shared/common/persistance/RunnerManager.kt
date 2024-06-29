package no.iktdev.mediaprocessing.shared.common.persistance

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import no.iktdev.mediaprocessing.shared.common.datasource.executeOrException
import no.iktdev.mediaprocessing.shared.common.datasource.withDirtyRead
import no.iktdev.mediaprocessing.shared.common.getAppVersion
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.UUID

class RunnerManager(private val dataSource: DataSource, private val startId: String = UUID.randomUUID().toString(), val name: String) {
    private val log = KotlinLogging.logger {}

    fun assignRunner(): Boolean {
        return executeOrException(dataSource.database) {
            runners.insert {
                it[runners.startId] = this@RunnerManager.startId
                it[runners.application] = this@RunnerManager.name
                it[runners.version] = getAppVersion()
            }
        } == null
    }


    fun iAmSuperseded(): Boolean {
        return withDirtyRead(dataSource.database) {
            val runnerVersionCodes = runners.select {
                (runners.application eq this@RunnerManager.name) and
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