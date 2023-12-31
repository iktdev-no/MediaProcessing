package no.iktdev.streamit.content.ui.service

import dev.vishna.watchservice.KWatchEvent
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.ui.explorer.ExplorerCore
import no.iktdev.streamit.content.ui.fileRegister
import org.springframework.stereotype.Service
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

@Service
class FileRegisterService {
    val watcherChannel = CommonConfig.incomingContent.asWatchChannel()
    val core = ExplorerCore()

    fun fid(name: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(name.toByteArray())).toString(16).padStart(32, '0')
    }

    private fun addFileToIndex(it: KWatchEvent) {
        core.fromFile(it.file)?.let { info ->
            val fid = fid(it.file.name)
            fileRegister.put(fid, info)
        }
    }
    private fun indexItemsInFolder(it: File) {

    }

    private fun indexItems() {

    }

    init {
        Coroutines.io().launch {

        }
        Coroutines.io().launch {
            watcherChannel.consumeEach {
                when (it.kind) {
                    KWatchEvent.Kind.Created, KWatchEvent.Kind.Modified, KWatchEvent.Kind.Initialized  -> {
                        if (it.file.isDirectory) {
                            indexItemsInFolder(it.file)
                        } else {
                            addFileToIndex(it)
                        }
                    }
                    KWatchEvent.Kind.Deleted -> {
                        val fid = fid(it.file.name)
                        fileRegister.remove(fid)
                    }
                }
            }
        }
    }

}