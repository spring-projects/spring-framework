/*
 * Copyright 2002-2017 the original author or authors.
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

import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import reactor.core.publisher.Mono

/**
 * Extension for [ServerResponse.BodyBuilder.body] providing a `body(Publisher<T>)` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerResponse.BodyBuilder.body(publisher: Publisher<T>): Mono<ServerResponse> =
		body(publisher, object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [ServerResponse.BodyBuilder.body] providing a `bodyToServerSentEvents(Publisher<T>)` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerResponse.BodyBuilder.bodyToServerSentEvents(publisher: Publisher<T>): Mono<ServerResponse> =
		contentType(MediaType.TEXT_EVENT_STREAM).body(publisher, object : ParameterizedTypeReference<T>() {})
