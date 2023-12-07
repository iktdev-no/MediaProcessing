import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener

class TestMessageListener: DefaultMessageListener("nan") {
    override fun listen() {
    }
}