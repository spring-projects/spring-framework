/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.client

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Extension for [WebClient.RequestBodySpec.body] providing a `body<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any, S : Publisher<T>> RequestBodySpec.body(publisher: S): RequestHeadersSpec<*> =
		body(publisher, object : ParameterizedTypeReference<T>() {})

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
 * Coroutines variant of [WebClient.RequestHeadersSpec.exchange].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun WebClient.RequestHeadersSpec<out WebClient.RequestHeadersSpec<*>>.awaitResponse(): ClientResponse =
		exchange().awaitSingle()

/**
 * Coroutines variant of [WebClient.RequestBodySpec.body].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T: Any> WebClient.RequestBodySpec.body(crossinline supplier: suspend () -> T)
		= body(GlobalScope.mono { supplier.invoke() })

/**
 * Coroutines variant of [WebClient.ResponseSpec.bodyToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend inline fun <reified T : Any> WebClient.ResponseSpec.awaitBody() : T =
		bodyToMono<T>().awaitSingle()
