export type SystemStatus = {
    coordinatorRest: boolean
    coordinatorSse: boolean
    processer: boolean
    converter: boolean
    pyMetadata: boolean
    pyWatcher: boolean
    timestamp: number
    interval: number
}
