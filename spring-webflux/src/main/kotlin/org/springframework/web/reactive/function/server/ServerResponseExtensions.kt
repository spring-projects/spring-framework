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

package org.springframework.web.reactive.function.server

import kotlinx.coroutines.reactive.awaitSingle
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import reactor.core.publisher.Mono

/**
 * Extension for [ServerResponse.BodyBuilder.body] providing a `body(Publisher<T>)`
 * variant. This extension is not subject to type erasure and retains actual generic
 * type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerResponse.BodyBuilder.body(publisher: Publisher<T>): Mono<ServerResponse> =
		body(publisher, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [ServerResponse.BodyBuilder.body] providing a
 * `bodyToServerSentEvents(Publisher<T>)` variant. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerResponse.BodyBuilder.bodyToServerSentEvents(publisher: Publisher<T>): Mono<ServerResponse> =
		contentType(MediaType.TEXT_EVENT_STREAM).body(publisher, object : ParameterizedTypeReference<T>() {})

/**
 * Shortcut for setting [MediaType.APPLICATION_JSON_UTF8] `Content-Type` header.
 * @author Sebastien Deleuze
 * @since 5.1
 */
fun ServerResponse.BodyBuilder.json() = contentType(MediaType.APPLICATION_JSON_UTF8)

/**
 * Shortcut for setting [MediaType.APPLICATION_XML] `Content-Type` header.
 * @author Sebastien Deleuze
 * @since 5.1
 */
fun ServerResponse.BodyBuilder.xml() = contentType(MediaType.APPLICATION_XML)

/**
 * Shortcut for setting [MediaType.TEXT_HTML] `Content-Type` header.
 * @author Sebastien Deleuze
 * @since 5.1
 */
fun ServerResponse.BodyBuilder.html() = contentType(MediaType.TEXT_HTML)

/**
 * Coroutines variant of [ServerResponse.HeadersBuilder.build].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerResponse.HeadersBuilder<out ServerResponse.HeadersBuilder<*>>.buildAndAwait(): ServerResponse =
		build().awaitSingle()

/**
 * Coroutines variant of [ServerResponse.BodyBuilder.syncBody].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerResponse.BodyBuilder.bodyAndAwait(body: Any): ServerResponse =
		syncBody(body).awaitSingle()


/**
 * Coroutines variant of [ServerResponse.BodyBuilder.syncBody] without the sync prefix since it is ok to use it within
 * another suspendable function.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerResponse.BodyBuilder.renderAndAwait(name: String, vararg modelAttributes: String): ServerResponse =
		render(name, *modelAttributes).awaitSingle()

/**
 * Coroutines variant of [ServerResponse.BodyBuilder.syncBody] without the sync prefix since it is ok to use it within
 * another suspendable function.
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerResponse.BodyBuilder.renderAndAwait(name: String, model: Map<String, *>): ServerResponse =
		render(name, model).awaitSingle()
