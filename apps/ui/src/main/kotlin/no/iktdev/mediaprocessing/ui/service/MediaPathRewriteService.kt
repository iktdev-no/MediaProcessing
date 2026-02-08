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

            if (cfg.scratchRewrite != null)
                log.warn { " - cache rewrite: ${cfg.scratch} → ${cfg.scratchRewrite!!.to}" }

            if (cfg.intermediateRewrite != null)
                log.warn { " - intermediate rewrite: ${cfg.intermediate} → ${cfg.intermediateRewrite!!.to}" }

            if (cfg.outboxRewrite != null)
                log.warn { " - outgoing rewrite: ${cfg.outbox} → ${cfg.outboxRewrite!!.to}" }

            if (cfg.inboxRewrite != null)
                log.warn { " - incoming rewrite: ${cfg.inbox} → ${cfg.inboxRewrite!!.to}" }

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
            path.startsWith(cfg.scratch) && cfg.scratchRewrite != null ->
                path.replaceFirst(cfg.scratch, cfg.scratchRewrite!!.to)

            path.startsWith(cfg.intermediate) && cfg.intermediateRewrite != null ->
                path.replaceFirst(cfg.intermediate, cfg.intermediateRewrite!!.to)

            path.startsWith(cfg.outbox) && cfg.outboxRewrite != null ->
                path.replaceFirst(cfg.outbox, cfg.outboxRewrite!!.to)

            path.startsWith(cfg.inbox) && cfg.inboxRewrite != null ->
                path.replaceFirst(cfg.inbox, cfg.inboxRewrite!!.to)

            else -> path
        }

        if (rewritten != path) {
            log.info { "Rewriting media path: '$path' → '$rewritten'" }
        }

        return rewritten
    }
}

