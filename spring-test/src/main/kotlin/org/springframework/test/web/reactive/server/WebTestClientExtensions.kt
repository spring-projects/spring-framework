/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server

import kotlinx.coroutines.flow.Flow
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
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
 * Extension for [ResponseSpec.expectBody] providing an `expectBody<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
inline fun <reified B : Any> ResponseSpec.expectBody(): BodySpec<B, *> =
		expectBody(object : ParameterizedTypeReference<B>() {})

/**
 * Extension for [ResponseSpec.expectBodyList] providing a `expectBodyList<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified E : Any?> ResponseSpec.expectBodyList(): ListBodySpec<E> =
		expectBodyList(object : ParameterizedTypeReference<E>() {})

/**
 * Extension for [ResponseSpec.returnResult] providing a `returnResult<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ResponseSpec.returnResult(): FluxExchangeResult<T> =
		returnResult(object : ParameterizedTypeReference<T>() {})
