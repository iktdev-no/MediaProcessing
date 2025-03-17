package no.iktdev.mediaprocessing.ui.socket.a2a

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.ui.UIEnv
import no.iktdev.mediaprocessing.ui.WebSocketMonitoringService
import no.iktdev.mediaprocessing.ui.log
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.*
import org.springframework.stereotype.Service
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type

@Service
class ProcesserListenerService(
    @Autowired private val webSocketMonitoringService: WebSocketMonitoringService,
    @Autowired private val message: SimpMessagingTemplate?,
) {
    private val logger = KotlinLogging.logger {}
    private val listeners: MutableList<A2AProcesserListener> = mutableListOf()

    fun attachListener(listener: A2AProcesserListener) {
        listeners.add(listener)
    }

    val gson = Gson()

    val client = WebSocketStompClient(StandardWebSocketClient())

    init {
        connectAndListen()
    }

    private final fun connectAndListen() {
        log.info { "EncoderWsUrl: ${UIEnv.socketEncoder}" }
        client.connect(UIEnv.socketEncoder, object : StompSessionHandlerAdapter() {
            override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
                super.afterConnected(session, connectedHeaders)
                logger.info { "Tilkoblet processer" }
                subscribeToTopics(session)
            }

            override fun handleException(
                session: StompSession,
                command: StompCommand?,
                headers: StompHeaders,
                payload: ByteArray,
                exception: Throwable
            ) {
                super.handleException(session, command, headers, payload, exception)
                logger.error { "Feil ved tilkobling: ${exception.message}" }
            }
        })
    }

    private fun subscribeToTopics(session: StompSession) {
        session.subscribe("/topic/encode/progress", encodeProcessFrameHandler)
        session.subscribe("/topic/extract/progress", extractProcessFrameHandler)
    }

    private val encodeProcessFrameHandler = object : StompFrameHandler {
        override fun getPayloadType(headers: StompHeaders): Type {
            return ProcesserEventInfo::class.java
        }

        override fun handleFrame(headers: StompHeaders, payload: Any?) {
            val response = gson.fromJson(payload.toString(), ProcesserEventInfo::class.java)
            if (webSocketMonitoringService.anyListening()) {
                message?.convertAndSend("/topic/processer/encode/progress", response)
            }
        }
    }

    private val extractProcessFrameHandler = object : StompFrameHandler {
        override fun getPayloadType(headers: StompHeaders): Type {
            return ProcesserEventInfo::class.java
        }

        override fun handleFrame(headers: StompHeaders, payload: Any?) {
            val response = gson.fromJson(payload.toString(), ProcesserEventInfo::class.java)
            if (webSocketMonitoringService.anyListening()) {
                message?.convertAndSend("/topic/processer/extract/progress", response)
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