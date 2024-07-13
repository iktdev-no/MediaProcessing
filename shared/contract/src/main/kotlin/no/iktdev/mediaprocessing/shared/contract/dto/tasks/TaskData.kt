package no.iktdev.mediaprocessing.shared.contract.dto.tasks

import java.io.Serializable

abstract class TaskData(): Serializable {
    abstract val inputFile: String

}