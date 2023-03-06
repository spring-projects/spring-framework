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

package org.springframework.web.reactive.function.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.asFlow
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

/**
 * Extension for [ClientResponse.bodyToMono] providing a `bodyToMono<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.bodyToMono(): Mono<T> =
		bodyToMono(object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [ClientResponse.bodyToFlux] providing a `bodyToFlux<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.bodyToFlux(): Flux<T> =
		bodyToFlux(object : ParameterizedTypeReference<T>() {})

/**
 * Coroutines [kotlinx.coroutines.flow.Flow] based variant of [ClientResponse.bodyToFlux].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
inline fun <reified T : Any> ClientResponse.bodyToFlow(): Flow<T> =
		bodyToFlux<T>().asFlow()

/**
 * `KClass` coroutines [kotlinx.coroutines.flow.Flow] based variant of [ClientResponse.bodyToFlux].
 * Please consider `bodyToFlow<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
fun <T : Any> ClientResponse.bodyToFlow(clazz: KClass<T>): Flow<T> =
		bodyToFlux(clazz.java).asFlow()

/**
 * Extension for [ClientResponse.toEntity] providing a `toEntity<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.toEntity(): Mono<ResponseEntity<T>> =
		toEntity(object : ParameterizedTypeReference<T>() {})

/**
 * Extension for [ClientResponse.toEntityList] providing a `bodyToEntityList<Foo>()` variant
 * leveraging Kotlin reified type parameters. This extension is not subject to type
 * erasure and retains actual generic type arguments.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.toEntityList(): Mono<ResponseEntity<List<T>>> =
		toEntityList(object : ParameterizedTypeReference<T>() {})

/**
 * Non-nullable Coroutines variant of [ClientResponse.bodyToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend inline fun <reified T : Any> ClientResponse.awaitBody(): T =
		bodyToMono<T>().awaitSingle()

/**
 * `KClass` non-nullable coroutines variant of [ClientResponse.bodyToMono].
 * Please consider `awaitBody<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
suspend fun <T : Any> ClientResponse.awaitBody(clazz: KClass<T>): T =
		bodyToMono(clazz.java).awaitSingle()

/**
 * Nullable coroutines variant of [ClientResponse.bodyToMono].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
@Suppress("DEPRECATION")
suspend inline fun <reified T : Any> ClientResponse.awaitBodyOrNull(): T? =
		bodyToMono<T>().awaitSingleOrNull()

/**
 * `KClass` nullable coroutines variant of [ClientResponse.bodyToMono].
 * Please consider `awaitBodyOrNull<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
@Suppress("DEPRECATION")
suspend fun <T : Any> ClientResponse.awaitBodyOrNull(clazz: KClass<T>): T? =
		bodyToMono(clazz.java).awaitSingleOrNull()

/**
 * Coroutines variant of [ClientResponse.toEntity].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend inline fun <reified T : Any> ClientResponse.awaitEntity(): ResponseEntity<T> =
		toEntity<T>().awaitSingle()

/**
 * `KClass` coroutines variant of [ClientResponse.toEntity].
 * Please consider `awaitEntity<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
suspend fun <T : Any> ClientResponse.awaitEntity(clazz: KClass<T>): ResponseEntity<T> =
		toEntity(clazz.java).awaitSingle()

/**
 * Coroutines variant of [ClientResponse.toEntityList].
 *
 * @author Sebastien Deleuze
 * @since 5.2
 */
suspend inline fun <reified T : Any> ClientResponse.awaitEntityList(): ResponseEntity<List<T>> =
		toEntityList<T>().awaitSingle()

/**
 * `KClass` coroutines variant of [ClientResponse.toEntityList].
 * Please consider `awaitEntityList<Foo>` variant if possible.
 *
 * @author Igor Manushin
 * @since 5.3
 */
suspend fun <T : Any> ClientResponse.awaitEntityList(clazz: KClass<T>): ResponseEntity<List<T>> =
		toEntityList(clazz.java).awaitSingle()

/**
 * Coroutines variant of [ClientResponse.toBodilessEntity].
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
suspend fun ClientResponse.awaitBodilessEntity(): ResponseEntity<Void> =
		toBodilessEntity().awaitSingle()

/**
 * Coroutines variant of [ClientResponse.createException].
 *
 * @author Sebastien Deleuze
 * @since 5.3
 */
suspend fun ClientResponse.createExceptionAndAwait(): WebClientResponseException =
		createException().awaitSingle()


