package de.privat.schmuddelwetter.data.dwd

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

/**
 * Umhüllt einen OkHttp-Response-Body und meldet die bisher gelesenen Bytes –
 * damit die UI bei einem großen Download (z. B. der ~36-MB-DWD-Sammeldatei)
 * einen echten Fortschritt statt eines reglosen Ladekreises anzeigen kann.
 */
class ProgressResponseBody(
    private val delegate: ResponseBody,
    private val onProgress: (bytesRead: Long, totalBytes: Long) -> Unit,
) : ResponseBody() {

    private val bufferedSource: BufferedSource by lazy { trackingSource(delegate.source()).buffer() }

    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()
    override fun source(): BufferedSource = bufferedSource

    private fun trackingSource(source: Source): Source = object : ForwardingSource(source) {
        var totalBytesRead = 0L

        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead != -1L) totalBytesRead += bytesRead
            // Qualifiziert, weil ForwardingSource selbst ein Member "delegate" hat,
            // das das äußere ResponseBody-Feld sonst überschatten würde.
            onProgress(totalBytesRead, this@ProgressResponseBody.delegate.contentLength())
            return bytesRead
        }
    }
}
