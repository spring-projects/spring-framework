/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.reactive.server

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.util.AssertionErrors.assertEquals
import org.springframework.test.web.reactive.server.WebTestClient.*

/**
 * Extension for [RequestBodySpec.body] providing a variant without explicit class
 * parameter thanks to Kotlin reified type parameters.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any, S : Publisher<T>> RequestBodySpec.body(publisher: S): RequestHeadersSpec<*>
		= body(publisher, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [RequestBodySpec.body] providing a `body<T>(Any)` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 * @param producer the producer to write to the request. This must be a
 * [Publisher] or another producer adaptable to a
 * [Publisher] via [org.springframework.core.ReactiveAdapterRegistry]
 * @param T the type of the elements contained in the producer
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> RequestBodySpec.body(producer: Any): RequestHeadersSpec<*>
		= body(producer, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [RequestBodySpec.body] providing a `body(Flow<T>)` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 * @param flow the [Flow] to write to the request
 * @param T the type of the elements contained in the publisher
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> RequestBodySpec.body(flow: Flow<T>): RequestHeadersSpec<*> =
		body(flow, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [ResponseSpec.expectBody] providing an `expectBody<Foo>()` variant and
 * a workaround for [KT-5464](https://youtrack.jetbrains.com/issue/KT-5464) which
 * prevents to use `WebTestClient.BodySpec` in Kotlin.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified B : Any> ResponseSpec.expectBody(): KotlinBodySpec<B> =
		expectBody(object : ParameterizedTypeReference<B>() {}).returnResult().let {
			object : KotlinBodySpec<B> {

				override fun isEqualTo(expected: B): KotlinBodySpec<B> = it
						.assertWithDiagnostics { assertEquals("Response body", expected, it.responseBody) }
						.let { this }

				override fun consumeWith(consumer: (EntityExchangeResult<B>) -> Unit): KotlinBodySpec<B> =
					it.assertWithDiagnostics { consumer.invoke(it) }.let { this }

				override fun returnResult(): EntityExchangeResult<B> = it
			}
		}

/**
 * Kotlin compliant `WebTestClient.BodySpec` for expectations on the response body decoded
 * to a single Object, see [KT-5464](https://youtrack.jetbrains.com/issue/KT-5464) for
 * more details.
 * @since 5.0.6
 */
interface KotlinBodySpec<B> {

	/**
	 * Assert the extracted body is equal to the given value.
	 */
	fun isEqualTo(expected: B): KotlinBodySpec<B>

	/**
	 * Assert the exchange result with the given consumer.
	 */
	fun consumeWith(consumer: (EntityExchangeResult<B>) -> Unit): KotlinBodySpec<B>

	/**
	 * Exit the chained API and return an `ExchangeResult` with the
	 * decoded response content.
	 */
	fun returnResult(): EntityExchangeResult<B>
}

/**
 * Extension for [ResponseSpec.expectBodyList] providing a `expectBodyList<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified E : Any> ResponseSpec.expectBodyList(): ListBodySpec<E> =
		expectBodyList(object : ParameterizedTypeReference<E>() {})

/**
 * Extension for [ResponseSpec.returnResult] providing a `returnResult<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ResponseSpec.returnResult(): FluxExchangeResult<T> =
		returnResult(object : ParameterizedTypeReference<T>() {})
