package no.iktdev.mediaprocessing.ui.service.sse

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import no.iktdev.mediaprocessing.ui.client.CoordinatorClient
import no.iktdev.mediaprocessing.ui.client.ProcesserClient
import org.springframework.stereotype.Service
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@Service
class SSEListeningService(
    private val coordinatorClient: CoordinatorClient,
    private val processerClient: ProcesserClient,
    private val sseEventHandler: SSEEventHandler,
) {

    // Status-trackere for om tjenestene er online
    private val coordinatorOnline = AtomicBoolean(false)
    private val processerOnline = AtomicBoolean(false)

    private var coordinatorSubscription: Disposable? = null
    private var processerSubscription: Disposable? = null

    @PostConstruct
    fun startListening() {
        val coordinatorStream = coordinatorClient.streamEvents()
            .doOnNext { event ->
                if (coordinatorOnline.compareAndSet(false, true)) {
                    println("✅ Ekte kontakt med Coordinator SSE!")
                }
                sseEventHandler.onEvent("Coordinator", event)
            }
            .doOnCancel {
                coordinatorOnline.set(false)
                println("❌ Coordinator SSE avbrutt")
            }
            .retryWhen(
                Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                    .maxBackoff(Duration.ofSeconds(30))
                    .doBeforeRetry { retrySignal ->
                        coordinatorOnline.set(false)
                        println("🔄 Mistet kontakt med Coordinator, forsøk nr. ${retrySignal.totalRetries() + 1}...")
                    }
            )

        val processerStream = processerClient.streamEvents()
            .doOnNext { event ->
                if (processerOnline.compareAndSet(false, true)) {
                    println("✅ Ekte kontakt med Processer SSE!")
                }
                sseEventHandler.onEvent("Processor", event)
            }
            .doOnCancel {
                processerOnline.set(false)
                println("❌ Processer SSE avbrutt")
            }
            .retryWhen(
                Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(2))
                    .maxBackoff(Duration.ofSeconds(30))
                    .doBeforeRetry { retrySignal ->
                        processerOnline.set(false)
                        println("🔄 Mistet kontakt med Processer, forsøk nr. ${retrySignal.totalRetries() + 1}...")
                    }
            )

        // Lagre referansene slik at vi kan rydde opp etter oss
        coordinatorSubscription = coordinatorStream.subscribe()
        processerSubscription = processerStream.subscribe()
    }

    @PreDestroy
    fun stopListening() {
        coordinatorSubscription?.dispose()
        processerSubscription?.dispose()
    }

    // Metoder for å sjekke om klientene er online
    fun isCoordinatorOnline(): Boolean = coordinatorOnline.get()
    fun isProcesserOnline(): Boolean = processerOnline.get()
}