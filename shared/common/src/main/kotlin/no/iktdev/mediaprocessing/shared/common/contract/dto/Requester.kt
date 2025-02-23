package no.iktdev.mediaprocessing.shared.common.contract.dto

abstract class Requester {
    open val source: String = "Unset"
}