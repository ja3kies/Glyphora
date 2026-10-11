package com.glyphora.presentation.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DebouncedProgressWriter<T>(
    private val scope: CoroutineScope,
    private val debounceMillis: Long,
    private val write: suspend (T) -> Unit
) {
    private data class Pending<T>(val value: T)

    private val writeMutex = Mutex()
    private val stateLock = Any()
    private var pending: Pending<T>? = null
    private var scheduledWrite: Job? = null

    fun schedule(value: T) {
        synchronized(stateLock) {
            pending = Pending(value)
            scheduledWrite?.cancel()
            scheduledWrite = scope.launch {
                delay(debounceMillis)
                writePending()
            }
        }
    }

    suspend fun flush() {
        val scheduled = synchronized(stateLock) { scheduledWrite }
        scheduled?.cancelAndJoin()
        synchronized(stateLock) {
            if (scheduledWrite === scheduled) scheduledWrite = null
        }
        writePending()
    }

    private suspend fun writePending() {
        writeMutex.withLock {
            val snapshot = synchronized(stateLock) {
                pending ?: return
            }
            write(snapshot.value)
            synchronized(stateLock) {
                if (pending === snapshot) {
                    pending = null
                    scheduledWrite = null
                }
            }
        }
    }
}
