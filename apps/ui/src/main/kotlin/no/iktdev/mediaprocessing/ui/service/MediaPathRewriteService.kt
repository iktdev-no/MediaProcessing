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

            cfg.scratchRewrite?.let { log.warn { " - cache rewrite: ${cfg.scratch} → ${it.to}" } }
            cfg.intermediateRewrite?.let { log.warn { " - intermediate rewrite: ${cfg.intermediate} → ${it.to}" } }
            cfg.outboxRewrite?.let { log.warn { " - outgoing rewrite: ${cfg.outbox} → ${it.to}" } }
            cfg.inboxRewrite?.let { log.warn { " - incoming rewrite: ${cfg.inbox} → ${it.to}" } }

        } else {
            log.info { "MediaPathRewriteService is INACTIVE (profiles: ${env.activeProfiles.joinToString()})" }
        }
    }

    fun rewrite(path: String): String {
        val normalizedInput = normalizePath(path)

        if (!env.activeProfiles.any { it == "dev" || it == "local" }) {
            return normalizedInput
        }

        val rewritten = rewriteWithSlashSafety(normalizedInput)
        val normalized = normalizePath(rewritten)

        if (normalized != path) {
            log.info { "Rewriting media path: '$path' → '$normalized'" }
        }

        return normalized
    }

    private fun rewriteWithSlashSafety(path: String): String {
        fun rewriteSegment(from: String, to: String?): String? {
            if (to == null) return null

            val fromNorm = normalizePath(from).removeSuffix("/")
            val toNorm = normalizePath(to).removeSuffix("/")

            val fromWithSlash = "$fromNorm/"
            val toWithSlash = "$toNorm/"

            return if (path.startsWith(fromWithSlash)) {
                path.replaceFirst(fromWithSlash, toWithSlash)
            } else null
        }

        return rewriteSegment(cfg.scratch, cfg.scratchRewrite?.to)
            ?: rewriteSegment(cfg.intermediate, cfg.intermediateRewrite?.to)
            ?: rewriteSegment(cfg.outbox, cfg.outboxRewrite?.to)
            ?: rewriteSegment(cfg.inbox, cfg.inboxRewrite?.to)
            ?: path
    }

    private fun normalizePath(path: String): String {
        if (path.isBlank()) return path

        var p = path.replace("\\", "/")

        val prefix = if (p.startsWith("//")) "//" else ""
        p = p.removePrefix("//")

        while ("//" in p) {
            p = p.replace("//", "/")
        }

        return prefix + p
    }
}
