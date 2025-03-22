package no.iktdev.mediaprocessing.ui.socket.a2a

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.ui.UIEnv
import no.iktdev.mediaprocessing.ui.WebSocketMonitoringService
import no.iktdev.mediaprocessing.ui.log
import no.iktdev.mediaprocessing.ui.socket.SocketClient
import no.iktdev.mediaprocessing.ui.socket.SocketMessageHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class ProcesserListenerService(
    @Autowired private val webSocketMonitoringService: WebSocketMonitoringService,
) {
    private val logger = KotlinLogging.logger {}
    private val listeners: MutableList<A2AProcesserListener> = mutableListOf()

    private var socketClient: SocketClient? = null

    fun attachListener(listener: A2AProcesserListener) {
        listeners.add(listener)
    }

    val gson = Gson()

    private final val socketEvent = object : SocketClient.SocketEvents {
        override fun onConnected() {
            super.onConnected()
            log.info { "EncoderWsUrl: ${UIEnv.socketEncoder}" }
            logger.info { "Tilkoblet processer" }

            socketClient?.subscribe("/topic/encode/progress", encodeProcessMessage)
            socketClient?.subscribe("/topic/extract/progress", extractProcessFrameHandler)
        }
    }

    init {
        SocketClient(UIEnv.socketEncoder, socketEvent).also {
            it.connect()
            this.socketClient = it
        }
    }


    private val encodeProcessMessage = object : SocketMessageHandler() {
        override fun onMessage(socketMessage: String) {
            super.onMessage(socketMessage)
            val response = gson.fromJson(socketMessage, ProcesserEventInfo::class.java)
            listeners.forEach { listener ->
                run {
                    listener.onEncodeProgress(response)
                }
            }
        }
    }


    private val extractProcessFrameHandler = object : SocketMessageHandler() {
        override fun onMessage(socketMessage: String) {
            super.onMessage(socketMessage)
            if (webSocketMonitoringService.anyListening()) {
            }
            val response = gson.fromJson(socketMessage, ProcesserEventInfo::class.java)
            listeners.forEach { listener ->
                run {
                    listener.onEncodeProgress(response)
                }
            }
        }
    }

    interface A2AProcesserListener {
        fun onExtractProgress(info: ProcesserEventInfo)
        fun onEncodeProgress(info: ProcesserEventInfo)
        fun onEncodeAssigned()
        fun onExtractAssigned()
    }

}