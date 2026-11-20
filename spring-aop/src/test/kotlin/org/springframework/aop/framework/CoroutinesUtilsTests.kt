/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.coroutines.Continuation

/**
 * Tests for [CoroutinesUtils].
 *
 * @author Sebastien Deleuze
 */
class CoroutinesUtilsTests {

	@Test
	fun awaitSingleNonNullValue() {
		val value = "foo"
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		assertThat(CoroutinesUtils.awaitSingleOrNull(value, continuation)).isEqualTo(value)
	}

	@Test
	fun awaitSingleNullValue() {
		val value = null
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		assertThat(CoroutinesUtils.awaitSingleOrNull(value, continuation)).isNull()
	}

	@Test
	fun awaitSingleMonoValue() {
		val value = "foo"
		val continuation = Continuation<Any>(CoroutineName("test")) { }
		assertThat(CoroutinesUtils.awaitSingleOrNull(Mono.just(value), continuation)).isEqualTo(value)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	suspend fun flow() {
		val value1 = "foo"
		val value2 = "bar"
		val values = Flux.just(value1, value2)
		val flow = CoroutinesUtils.asFlow(values) as Flow<String>
		assertThat(flow.toList()).containsExactly(value1, value2)
	}

}
