package no.iktdev.mediaprocessing.shared.common.persistance

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object processerEvents: IntIdTable() {

    val claimed: Column<Boolean> = bool("claimed")
    val data: Column<String> = text("data")
}