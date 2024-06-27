package no.iktdev.mediaprocessing.shared.common.persistance

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.datasource.DataSource
import no.iktdev.mediaprocessing.shared.common.datasource.executeOrException
import no.iktdev.mediaprocessing.shared.common.datasource.withDirtyRead
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.UUID

class RunnerManager(private val dataSource: DataSource, val startId: String = UUID.randomUUID().toString(), val name: String, val version: String) {
    private val log = KotlinLogging.logger {}

    fun assignRunner(): Boolean {
        return executeOrException(dataSource.database) {
            runners.insert {
                it[runners.startId] = this@RunnerManager.startId
                it[runners.application] = this@RunnerManager.name
                it[runners.version] = this@RunnerManager.version
            }
        } == null
    }

    private fun versionToVersionCode(version: String?): Int? {
        return version?.replace(".", "")?.toIntOrNull()
    }

    fun iAmSuperseded(): Boolean {
        return withDirtyRead(dataSource.database) {
            val runnerVersionCodes = runners.select {
                (runners.application eq this@RunnerManager.version) and
                        (runners.startId neq this@RunnerManager.startId)
            }.map { it[runners.version] }.mapNotNull { versionToVersionCode(it) }
            val myVersion = versionToVersionCode(this.version)
            myVersion?.let {
                (runnerVersionCodes.any { rv -> rv > it })
            } ?: true
        } ?: true
    }
}

enum class ActiveMode {
    Active,
    Passive
}