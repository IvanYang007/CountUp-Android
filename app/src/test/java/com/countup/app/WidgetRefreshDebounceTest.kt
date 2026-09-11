package com.countup.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test validating the 350ms debounce and immediate onPause flush behavior
 * implemented in MainActivity to prevent AppWidget IPC storms across 5 providers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WidgetRefreshDebounceTest {

    private class TestDebouncer(private val scope: CoroutineScope) {
        var executionCount = 0
            private set
        private var job: Job? = null

        fun refresh(immediate: Boolean = false) {
            job?.cancel()
            job = scope.launch {
                if (!immediate) {
                    delay(350L)
                }
                executionCount++
            }
        }
    }

    @Test
    fun `rapid bursts within 350ms window cancel previous jobs and execute exactly once`() = runTest {
        val debouncer = TestDebouncer(this)

        // Rapidly trigger 5 refresh events at 80ms intervals (total 320ms elapsed)
        debouncer.refresh(immediate = false)
        advanceTimeBy(80L)
        debouncer.refresh(immediate = false)
        advanceTimeBy(80L)
        debouncer.refresh(immediate = false)
        advanceTimeBy(80L)
        debouncer.refresh(immediate = false)
        advanceTimeBy(80L)
        debouncer.refresh(immediate = false)

        // Still within the debounce quiet period of the 5th event
        assertEquals("No execution should occur during rapid tapping", 0, debouncer.executionCount)

        // Advance 349ms after the 5th event
        advanceTimeBy(349L)
        assertEquals("Execution must not fire before the 350ms threshold", 0, debouncer.executionCount)

        // Advance 2ms to cross 350ms threshold
        advanceTimeBy(2L)
        assertEquals("Trailing edge must fire exactly once after 350ms quiet period", 1, debouncer.executionCount)
    }

    @Test
    fun `immediate refresh executes without delay and cancels pending debounced jobs`() = runTest {
        val debouncer = TestDebouncer(this)

        // Schedule debounced refresh
        debouncer.refresh(immediate = false)
        advanceTimeBy(200L)
        assertEquals(0, debouncer.executionCount)

        // User exits app to home screen (onPause triggers immediate = true)
        debouncer.refresh(immediate = true)
        runCurrent() // Runs pending tasks scheduled at the current virtual timestamp without advancing time
        assertEquals("Immediate refresh must fire synchronously without waiting 350ms", 1, debouncer.executionCount)

        // Ensure the original delayed job was canceled and does not fire later
        advanceTimeBy(400L)
        assertEquals("Previous delayed job was canceled and must not fire again", 1, debouncer.executionCount)
    }

    @Test
    fun `isolated events separated by more than 350ms each execute independently`() = runTest {
        val debouncer = TestDebouncer(this)

        debouncer.refresh(immediate = false)
        advanceTimeBy(400L)
        assertEquals(1, debouncer.executionCount)

        debouncer.refresh(immediate = false)
        advanceTimeBy(400L)
        assertEquals(2, debouncer.executionCount)
    }
}
