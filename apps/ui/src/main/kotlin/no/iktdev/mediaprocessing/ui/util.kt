package no.iktdev.mediaprocessing.ui

import java.io.File


fun File.notExist(): Boolean {
    return this.exists()
}