/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.function.server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.Part
import org.springframework.util.CollectionUtils
import org.springframework.util.MultiValueMap
import org.springframework.web.server.WebSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.security.Principal
import kotlin.reflect.KClass

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
 * Coroutines [kotlinx.coroutines.flow.Flow] based variant of [ServerRequest.bodyToFlux].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> ServerRequest.bodyToFlow(): Flow<T> =
		bodyToFlux<T>().asFlow()

/**
 * `KClass` coroutines [kotlinx.coroutines.flow.Flow] based variant of [ServerRequest.bodyToFlux].
 * Please consider `bodyToFlow<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
fun <T : Any> ServerRequest.bodyToFlow(clazz: KClass<T>): Flow<T> =
		bodyToFlux(clazz.java).asFlow()

/**
 * Non-nullable Coroutines variant of [ServerRequest.bodyToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend inline fun <reified T : Any> ServerRequest.awaitBody(): T =
		bodyToMono<T>().awaitSingle()

/**
 * `KClass` non-nullable Coroutines variant of [ServerRequest.bodyToMono].
 * Please consider `awaitBody<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
suspend fun <T : Any> ServerRequest.awaitBody(clazz: KClass<T>): T =
		bodyToMono(clazz.java).awaitSingle()

/**
 * Nullable Coroutines variant of [ServerRequest.bodyToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
@Suppress("DEPRECATION")
suspend inline fun <reified T : Any> ServerRequest.awaitBodyOrNull(): T? =
		bodyToMono<T>().awaitSingleOrNull()

/**
 * `KClass` nullable Coroutines variant of [ServerRequest.bodyToMono].
 * Please consider `awaitBodyOrNull<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
@Suppress("DEPRECATION")
suspend fun <T : Any> ServerRequest.awaitBodyOrNull(clazz: KClass<T>): T? =
		bodyToMono(clazz.java).awaitSingleOrNull()

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
@Suppress("DEPRECATION")
suspend fun ServerRequest.awaitPrincipal(): Principal? =
		principal().awaitSingleOrNull()

/**
 * Coroutines variant of [ServerRequest.session].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend fun ServerRequest.awaitSession(): WebSession =
		session().awaitSingle()

/**
 * Nullable variant of [ServerRequest.remoteAddress]
 *
 * @author Sebastien Deleuze
 * @since 5.2.2
 */
fun ServerRequest.remoteAddressOrNull(): InetSocketAddress? = remoteAddress().orElse(null)

/**
 * Nullable variant of [ServerRequest.attribute]
 *
 * @author Sebastien Deleuze
 * @since 5.2.2
 */
fun ServerRequest.attributeOrNull(name: String): Any? = attributes()[name]

/**
 * Nullable variant of [ServerRequest.queryParam]
 *
 * @author Sebastien Deleuze
 * @since 5.2.2
 */
fun ServerRequest.queryParamOrNull(name: String): String? {
	val queryParamValues = queryParams()[name]
	return if (CollectionUtils.isEmpty(queryParamValues)) {
		null
	} else {
		var value: String? = queryParamValues!![0]
		if (value == null) {
			value = ""
		}
		value
	}
}

/**
 * Nullable variant of [ServerRequest.Headers.contentLength]
 *
 * @author Sebastien Deleuze
 * @since 5.2.2
 */
fun ServerRequest.Headers.contentLengthOrNull(): Long? =
		contentLength().run { if (isPresent) asLong else null }

/**
 * Nullable variant of [ServerRequest.Headers.contentType]
 *
 * @author Sebastien Deleuze
 * @since 5.2.2
 */
fun ServerRequest.Headers.contentTypeOrNull(): MediaType? =
		contentType().orElse(null)
