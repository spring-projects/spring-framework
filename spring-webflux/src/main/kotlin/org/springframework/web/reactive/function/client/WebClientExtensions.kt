/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.reactive.function.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Extension for [WebClient.RequestBodySpec.body] providing a `body(Publisher<T>)` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any, S : Publisher<T>> RequestBodySpec.body(publisher: S): RequestHeadersSpec<*> =
		body(publisher, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [WebClient.RequestBodySpec.body] providing a `body(Flow<T>)` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 * @param flow the [Flow] to write to the request
 * @param T the type of the elements contained in the flow
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> RequestBodySpec.body(flow: Flow<T>): RequestHeadersSpec<*> =
		body(flow, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [WebClient.RequestBodySpec.body] providing a `body<T>(Any)` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 * @param producer the producer to write to the request. This must be a
 * [Publisher] or another producer adaptable to a
 * [Publisher] via [org.springframework.core.ReactiveAdapterRegistry]
 * @param T the type of the elements contained in the producer
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> RequestBodySpec.body(producer: Any): RequestHeadersSpec<*> =
		body(producer, object : ParameterizedTypeReference<T>() {})

/**
 * Coroutines variant of [WebClient.RequestHeadersSpec.exchange].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
@Suppress("DEPRECATION")
@Deprecated("Deprecated since 5.3 due to the possibility to leak memory and/or connections; please," +
		"use awaitExchange { } or exchangeToFlow { } instead; consider also using retrieve()" +
		"which provides access to the response status and headers via ResponseEntity along with error status handling.")
suspend fun RequestHeadersSpec<out RequestHeadersSpec<*>>.awaitExchange(): ClientResponse =
		exchange().awaitSingle()

/**
 * Coroutines variant of [WebClient.RequestHeadersSpec.exchangeToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
suspend fun <T: Any> RequestHeadersSpec<out RequestHeadersSpec<*>>.awaitExchange(responseHandler: suspend (ClientResponse) -> T): T =
		exchangeToMono { mono(Dispatchers.Unconfined) { responseHandler.invoke(it) } }.awaitSingle()

/**
 * Variant of [WebClient.RequestHeadersSpec.awaitExchange] that allows a nullable return
 *
 * @since 5.3.8
 */
@Suppress("DEPRECATION")
suspend fun <T: Any> RequestHeadersSpec<out RequestHeadersSpec<*>>.awaitExchangeOrNull(responseHandler: suspend (ClientResponse) -> T?): T? =
		exchangeToMono { mono(Dispatchers.Unconfined) { responseHandler.invoke(it) } }.awaitSingleOrNull()

/**
 * Coroutines variant of [WebClient.RequestHeadersSpec.exchangeToFlux].
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
fun <T: Any> RequestHeadersSpec<out RequestHeadersSpec<*>>.exchangeToFlow(responseHandler: (ClientResponse) -> Flow<T>): Flow<T> =
		exchangeToFlux { responseHandler.invoke(it).asFlux() }.asFlow()

/**
 * Extension for [WebClient.ResponseSpec.bodyToMono] providing a `bodyToMono<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> WebClient.ResponseSpec.bodyToMono(): Mono<T> =
		bodyToMono(object : ParameterizedTypeReference<T>() {})


/**
 * Extension for [WebClient.ResponseSpec.bodyToFlux] providing a `bodyToFlux<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> WebClient.ResponseSpec.bodyToFlux(): Flux<T> =
		bodyToFlux(object : ParameterizedTypeReference<T>() {})

/**
 * Coroutines [kotlinx.coroutines.flow.Flow] based variant of [WebClient.ResponseSpec.bodyToFlux].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> WebClient.ResponseSpec.bodyToFlow(): Flow<T> =
		bodyToFlux<T>().asFlow()

/**
 * Coroutines variant of [WebClient.ResponseSpec.bodyToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend inline fun <reified T : Any> WebClient.ResponseSpec.awaitBody() : T =
	when (T::class) {
		Unit::class -> awaitBodilessEntity().let { Unit as T }
		else -> bodyToMono<T>().awaitSingle()
	}

/**
 * Coroutines variant of [WebClient.ResponseSpec.bodyToMono].
 *
 * @author Valentin Shakhov
 * @since 5.3.6
 */
@Suppress("DEPRECATION")
suspend inline fun <reified T : Any> WebClient.ResponseSpec.awaitBodyOrNull() : T? =
	when (T::class) {
		Unit::class -> awaitBodilessEntity().let { Unit as T? }
		else -> bodyToMono<T>().awaitSingleOrNull()
	}

/**
 * Coroutines variant of [WebClient.ResponseSpec.toBodilessEntity].
 */
suspend fun WebClient.ResponseSpec.awaitBodilessEntity() =
	toBodilessEntity().awaitSingle()

/**
 * Extension for [WebClient.ResponseSpec.toEntity] providing a `toEntity<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @since 5.3.2
 */
inline fun <reified T : Any> WebClient.ResponseSpec.toEntity(): Mono<ResponseEntity<T>> =
		toEntity(object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [WebClient.ResponseSpec.toEntityList] providing a `toEntityList<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @since 5.3.2
 */
inline fun <reified T : Any> WebClient.ResponseSpec.toEntityList(): Mono<ResponseEntity<List<T>>> =
		toEntityList(object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [WebClient.ResponseSpec.toEntityFlux] providing a `toEntityFlux<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @since 5.3.2
 */
inline fun <reified T : Any> WebClient.ResponseSpec.toEntityFlux(): Mono<ResponseEntity<Flux<T>>> =
		toEntityFlux(object : ParameterizedTypeReference<T>() {})
