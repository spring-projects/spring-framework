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

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.server.WebSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.security.Principal

/**
 * Extension for [ServerRequest.bodyToMono] providing a `bodyToMono<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 * 
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerRequest.bodyToMono(): Mono<T> =
		bodyToMono(object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [ServerRequest.bodyToFlux] providing a `bodyToFlux<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerRequest.bodyToFlux(): Flux<T> =
		bodyToFlux(object : ParameterizedTypeReference<T>() {})

/**
 * Coroutines variant of [ServerRequest.bodyToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend inline fun <reified T : Any> ServerRequest.awaitBody(): T? =
		bodyToMono<T>().awaitFirstOrNull()

/**
 * Coroutines variant of [ServerRequest.formData].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerRequest.awaitFormData(): MultiValueMap<String, String> =
		formData().awaitSingle()

/**
 * Coroutines variant of [ServerRequest.multipartData].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerRequest.awaitMultipartData(): MultiValueMap<String, Part> =
		multipartData().awaitSingle()

/**
 * Coroutines variant of [ServerRequest.principal].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerRequest.awaitPrincipal(): Principal =
		principal().awaitSingle()

/**
 * Coroutines variant of [ServerRequest.session].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerRequest.awaitSession(): WebSession =
		session().awaitSingle()
