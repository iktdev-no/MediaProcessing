package no.iktdev.mediaprocessing.ui.socket

import mu.KotlinLogging
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SocketClient(private val url: String, val listener: SocketEvents? = null) {
    private val logger = KotlinLogging.logger {}

    private val client = WebSocketStompClient(StandardWebSocketClient())
    private val subscriptions: MutableList<SocketSubscription> = mutableListOf()
    private var session: StompSession? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var reconnectFuture: ScheduledFuture<*>? = null



    fun subscribe(topic: String, handler: StompSessionHandlerAdapter) {
        subscriptions.add(SocketSubscription(topic, handler))
        session?.subscribe(topic, handler)
    }


    private fun reconnect() {
        if (reconnectFuture != null && !reconnectFuture!!.isDone) return // Allerede reconnecting

        reconnectFuture?.cancel(true) // Kansellerer tidligere reconnect-task hvis den kjører

        logger.info { "Scheduling reconnect in 30 seconds" }

        reconnectFuture = scheduler.scheduleWithFixedDelay({
            try {
                logger.info { "Attempting to reconnect... $url" }
                listener?.onReconnecting()
                connect()
            } catch (e: Exception) {
                logger.error(e) { "Reconnect attempt failed" }
            }
        }, 5, 30, TimeUnit.SECONDS) // Starter etter 5 sekunder, med 30 sekunders intervall
    }

    private fun resetReconnector() {
        reconnectFuture?.cancel(true)
        reconnectFuture = null
    }

    fun disconnect() {
        if (!scheduler.isShutdown) {
            try {
                reconnectFuture?.cancel(true)
                scheduler.shutdownNow()
            } catch (e: Exception) {}
        }
        session?.disconnect()
        listener?.onDisconnected()
    }

    private val connectAdapter = object: StompSessionHandlerAdapter() {
        override fun afterConnected(session: StompSession, connectedHeaders: StompHeaders) {
            super.afterConnected(session, connectedHeaders)
            resetReconnector()
            listener?.onConnected()
            subscriptions.forEach {
                session.subscribe(it.destination, it.handler)
            }
        }

        override fun handleTransportError(session: StompSession, exception: Throwable) {
            super.handleTransportError(session, exception)
            listener?.onException(exception)
            this@SocketClient.session = null
            reconnect()
        }

        override fun handleException(
            session: StompSession,
            command: StompCommand?,
            headers: StompHeaders,
            payload: ByteArray,
            exception: Throwable
        ) {
            super.handleException(session, command, headers, payload, exception)
            listener?.onException(exception)
        }
    }

    fun connect() {
        client.connect(url, connectAdapter)
    }

    interface SocketEvents {
        fun onConnected(): Unit {}
        fun onReconnecting(): Unit {}
        fun onDisconnected(): Unit {}
        fun onException(e: Throwable) {}
    }
    data class SocketSubscription(
        val destination: String,
        val handler: StompSessionHandlerAdapter
    )
}