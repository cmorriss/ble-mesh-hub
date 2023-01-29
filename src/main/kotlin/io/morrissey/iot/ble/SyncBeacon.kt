package io.morrissey.iot.ble

import java.io.File
import java.nio.ByteBuffer
import java.time.Instant

class SyncBeacon(
    private val filePath: String,
    private val periodicityInSeconds: Long,
    private val firstSync: Long = Instant.now().toEpochMilli() + (periodicityInSeconds * 1000)
) : Runnable {
    private var nextSyncTime: Long = firstSync
    private var stop: Boolean = false

    init {
        Thread(this)
    }

    fun stop() {
        stop = true
    }

    override fun run() {
        File(filePath).outputStream().channel.use { fc ->
            while (!stop) {
                val timeRemaining = (nextSyncTime - System.currentTimeMillis()) / 1000L
                fc.position(0)
                fc.write(timeRemaining.toString().toByteBuffer())
                Thread.sleep(1000L)
            }
        }
    }

    private fun String.toByteBuffer() = ByteBuffer.wrap(toByteArray())
}
