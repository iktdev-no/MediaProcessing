package no.iktdev.streamit.content.ui.socket.internal

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.streamit.content.common.dto.WorkOrderItem
import no.iktdev.streamit.content.ui.UIEnv
import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.content.ui.memActiveEventMap
import no.iktdev.streamit.content.ui.memSimpleConvertedEventsMap
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.stereotype.Service
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.lang.reflect.Type

@Service
class EncoderReaderService {
    private val logger = KotlinLogging.logger {}


    fun startSubscription(session: StompSession) {
        session.subscribe("/topic/encoder/workorder", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type {
                return object : TypeToken<WorkOrderItem>() {}.type
                //return object : TypeToken<List<WorkOrderItem?>?>() {}.type
            }

            override fun handleFrame(headers: StompHeaders, payload: Any?) {
                if (payload is String) {
                    Gson().fromJson(payload, WorkOrderItem::class.java)?.let {
                        val item: EventDataObject = memActiveEventMap[it.id] ?: return
                        item.encode?.progress = it.progress
                        item.encode?.timeLeft = it.remainingTime
                        memActiveEventMap[it.id] = item;
                        memSimpleConvertedEventsMap[it.id] = item.toSimple()
                    }

                }

            }
        })
        session.subscribe("/topic/extractor/workorder", object : StompFrameHandler {
            override fun getPayloadType(headers: StompHeaders): Type {
                return object : TypeToken<WorkOrderItem>() {}.type
            }

            override fun handleFrame(headers: StompHeaders?, payload: Any?) {
                if (payload is String) {
                    val item = Gson().fromJson(payload, WorkOrderItem::class.java)


                }
            }
        })

    }


    val client = WebSocketStompClient(StandardWebSocketClient())
    val sessionHandler = object : StompSessionHandlerAdapter() {
        override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
            super.afterConnected(session, connectedHeaders)
            logger.info { "Connected to Encode Socket" }
            startSubscription(session)
        }

        override fun handleFrame(headers: StompHeaders, payload: Any?) {
            super.handleFrame(headers, payload)

        }
    }

    init {
        client.connect(UIEnv.socketEncoder, sessionHandler)
    }

}