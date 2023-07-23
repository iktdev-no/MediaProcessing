package no.iktdev.streamit.content.common.dto.reader.work

import java.util.UUID

abstract class WorkBase(
    @Transient open val workId: String = UUID.randomUUID().toString(),
    @Transient open val collection: String,
    @Transient open val inFile: String,
    @Transient open val outFile: String
)