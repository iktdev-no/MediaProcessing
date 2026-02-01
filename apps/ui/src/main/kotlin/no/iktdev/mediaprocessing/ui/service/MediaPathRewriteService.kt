package no.iktdev.mediaprocessing.ui.service

import mu.KotlinLogging
import no.iktdev.mediaprocessing.ui.MediaConfig
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

@Service
class MediaPathRewriteService(
    private val cfg: MediaConfig,
    private val env: Environment
) {

    private val log = KotlinLogging.logger {}

    init {
        val active = env.activeProfiles.any { it == "dev" || it == "local" }

        if (active) {
            log.warn { "MediaPathRewriteService is ACTIVE (profiles: ${env.activeProfiles.joinToString()})" }

            if (cfg.cacheRewrite != null)
                log.warn { " - cache rewrite: ${cfg.cache} → ${cfg.cacheRewrite!!.to}" }

            if (cfg.outgoingRewrite != null)
                log.warn { " - outgoing rewrite: ${cfg.outgoing} → ${cfg.outgoingRewrite!!.to}" }

            if (cfg.incomingRewrite != null)
                log.warn { " - incoming rewrite: ${cfg.incoming} → ${cfg.incomingRewrite!!.to}" }

        } else {
            log.info { "MediaPathRewriteService is INACTIVE (profiles: ${env.activeProfiles.joinToString()})" }
        }
    }

    fun rewrite(path: String): String {
        // Only active in dev/local
        if (!env.activeProfiles.any { it == "dev" || it == "local" }) {
            return path
        }

        val rewritten = when {
            path.startsWith(cfg.cache) && cfg.cacheRewrite != null ->
                path.replaceFirst(cfg.cache, cfg.cacheRewrite!!.to)

            path.startsWith(cfg.outgoing) && cfg.outgoingRewrite != null ->
                path.replaceFirst(cfg.outgoing, cfg.outgoingRewrite!!.to)

            path.startsWith(cfg.incoming) && cfg.incomingRewrite != null ->
                path.replaceFirst(cfg.incoming, cfg.incomingRewrite!!.to)

            else -> path
        }

        if (rewritten != path) {
            log.info { "Rewriting media path: '$path' → '$rewritten'" }
        }

        return rewritten
    }
}

