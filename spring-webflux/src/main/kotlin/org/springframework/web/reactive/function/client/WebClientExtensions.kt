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

package org.springframework.web.reactive.function.client

import org.reactivestreams.Publisher
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

/**
 * Extension for [WebClient.RequestBodySpec.body] providing a variant without explicit class
 * parameter thanks to Kotlin reified type parameters.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T : Any, S : Publisher<T>> WebClient.RequestBodySpec.body(publisher: S): WebClient.RequestHeadersSpec<*>
        = body(publisher, T::class.java)

/**
 * Extension for [WebClient.ResponseSpec.bodyToMono] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> WebClient.ResponseSpec.bodyToMono(type: KClass<T>): Mono<T> = bodyToMono(type.java)

/**
 * Extension for [WebClient.ResponseSpec.bodyToMono] providing a `bodyToMono<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> WebClient.ResponseSpec.bodyToMono(): Mono<T> = bodyToMono(T::class.java)


/**
 * Extension for [WebClient.ResponseSpec.bodyToFlux] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> WebClient.ResponseSpec.bodyToFlux(type: KClass<T>): Flux<T> = bodyToFlux(type.java)

/**
 * Extension for [WebClient.ResponseSpec.bodyToFlux] providing a `bodyToFlux<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> WebClient.ResponseSpec.bodyToFlux(): Flux<T> = bodyToFlux(T::class.java)

/**
 * Extension for [WebClient.ResponseSpec.bodyToEntity] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> WebClient.ResponseSpec.bodyToEntity(type: KClass<T>): Mono<ResponseEntity<T>> = bodyToEntity(type.java)

/**
 * Extension for [WebClient.ResponseSpec.bodyToEntity] providing a `bodyToEntity<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> WebClient.ResponseSpec.bodyToEntity(): Mono<ResponseEntity<T>> = bodyToEntity(T::class.java)

/**
 * Extension for [WebClient.ResponseSpec.bodyToEntityList] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> WebClient.ResponseSpec.bodyToEntityList(type: KClass<T>): Mono<ResponseEntity<List<T>>> = bodyToEntityList(type.java)

/**
 * Extension for [WebClient.ResponseSpec.bodyToEntityList] providing a `bodyToEntityList<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> WebClient.ResponseSpec.bodyToEntityList(): Mono<ResponseEntity<List<T>>> = bodyToEntityList(T::class.java)
