package com.glyphora.presentation.reader

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebouncedProgressWriterTest {
    @Test
    fun rapidUpdatesWriteOnlyTheLatestValueAfterDebounce() = runTest {
        val writes = mutableListOf<Int>()
        val writer = DebouncedProgressWriter<Int>(this, 500) { writes += it }

        writer.schedule(1)
        advanceTimeBy(400)
        writer.schedule(2)
        advanceTimeBy(499)
        runCurrent()
        assertEquals(emptyList<Int>(), writes)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(2), writes)
    }

    @Test
    fun flushWritesPendingValueImmediatelyAndCancelsScheduledWrite() = runTest {
        val writes = mutableListOf<Int>()
        val writer = DebouncedProgressWriter<Int>(this, 500) { writes += it }

        writer.schedule(7)
        writer.flush()
        runCurrent()

        assertEquals(listOf(7), writes)
    }
}
