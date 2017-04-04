/*
 * Copyright 2002-2017 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util.experimental

import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.springframework.util.concurrent.ListenableFuture
import org.springframework.util.concurrent.SettableListenableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException

/**
 * Tests for coroutine support for [ListenableFuture].
 *
 * @author Konrad Kamiński
 */
class ListenableFutureExtensionsTests {
    @Test
    fun `listenableFuture from successful suspending lambda`() {
        val string = "OK"
        val listenableFuture = listenableFuture { string }

        listenableFuture.assertThat(value = string, cancelled = false, done = true)
    }

    @Test
    fun `listenableFuture from failing suspending lambda`() {
        val exception = RuntimeException("message")
        val listenableFuture = listenableFuture { throw exception }

        listenableFuture.assertThat(exception = exception, cancelled = false, done = true)
    }

    @Test
    fun `listenableFuture waiting for successful completion`() {
        val string = "OK"
        val latch = CountDownLatch(1)

        val listenableFuture = listenableFuture {
            latch.await()
            string
        }

        listenableFuture.assertThat(cancelled = false, done = false)

        latch.countDown()
        listenableFuture.assertThat(value = string, cancelled = false, done = true)
    }

    @Test
    fun `listenableFuture waiting for failing completion`() {
        val exception = RuntimeException("message")
        val latch = CountDownLatch(1)

        val listenableFuture = listenableFuture {
            latch.await()
            throw exception
        }

        listenableFuture.assertThat(cancelled = false, done = false)

        latch.countDown()
        listenableFuture.assertThat(exception = exception, cancelled = false, done = true)
    }

    @Test
    fun `listenableFuture waiting for cancellation`() {
        val latch = CountDownLatch(1)
        val listenableFuture = listenableFuture {
            latch.await()
        }

        listenableFuture.cancel(true)

        listenableFuture.assertThat(exceptionAssert = { it is CancellationException}, cancelled = true, done = true)
        latch.countDown()
    }

    @Test
    fun `Successful Deferred converted to ListenableFuture`() {
        val string = "OK"
        val deferred = async(CommonPool) {
            string
        }

        val listenableFuture = deferred.asListenableFuture()

        listenableFuture.assertThat(value = string, cancelled = false, done = true)
    }

    @Test
    fun `Failing Deferred converted to ListenableFuture`() {
        val exception = RuntimeException("message")
        val deferred = async(CommonPool) {
            throw exception
        }

        val listenableFuture = deferred.asListenableFuture()

        listenableFuture.assertThat(exception = exception, cancelled = false, done = true)
    }

    @Test
    fun `Deferred waiting for success converted to ListenableFuture`() {
        val string = "OK"
        val latch = CountDownLatch(1)
        val deferred = async(CommonPool) {
            latch.await()
            string
        }

        val listenableFuture = deferred.asListenableFuture()
        listenableFuture.assertThat(cancelled = false, done = false)

        latch.countDown()
        listenableFuture.assertThat(value = string, cancelled = false, done = true)
    }

    @Test
    fun `Deferred waiting for failure converted to ListenableFuture`() {
        val exception = RuntimeException("message")
        val latch = CountDownLatch(1)
        val deferred = async(CommonPool) {
            latch.await()
            throw exception
        }

        val listenableFuture = deferred.asListenableFuture()

        listenableFuture.assertThat(cancelled = false, done = false)

        latch.countDown()

        listenableFuture.assertThat(exception = exception, cancelled = false, done = true)
    }

    @Test
    fun `awaiting successful ListenableFuture`() = runBlocking {
        val string = "OK"
        val listenableFuture = SettableListenableFuture<String>().apply {
            set(string)
        }

        assertEquals(string, listenableFuture.await())
    }

    @Test
    fun `awaiting failing ListenableFuture`() = runBlocking {
        val exception = RuntimeException("message")
        val listenableFuture = SettableListenableFuture<String>().apply {
            setException(exception)
        }

        try {
            listenableFuture.await()
            fail("Expected Exception")
        }
        catch (e: Exception) {
            assertEquals(exception, e)
        }
    }

    @Test
    fun `awaiting cancelled ListenableFuture`() = runBlocking {
        val listenableFuture = SettableListenableFuture<String>().apply {
            cancel(true)
        }

        try {
            listenableFuture.await()
            fail("Expected Exception")
        }
        catch (e: Exception) {
            assertTrue(e is CancellationException)
        }
    }

    private fun <T> ListenableFuture<T>.assertThat(cancelled: Boolean, done: Boolean,
        value: T? = null, exception: Exception? = null, exceptionAssert: ((Exception) -> Unit)? = null) {

        if (value != null) {
            assertEquals(value, get())
        }
        if (exception != null || exceptionAssert != null) {
            try {
                get()
                fail("Expected ExecutionException")
            }
            catch (e: Exception) {
                if (exceptionAssert != null) {
                    exceptionAssert(e)
                }
                if (e is ExecutionException) {
                    assertEquals(exception, e.cause)
                }
            }
        }

        assertEquals(cancelled, isCancelled)
        assertEquals(done, isDone)
    }
}