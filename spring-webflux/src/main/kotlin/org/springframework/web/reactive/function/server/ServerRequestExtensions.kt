package org.springframework.web.reactive.function.server

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass


/**
 * Extension for [ServerRequest.bodyToMono] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ServerRequest.bodyToMono(type: KClass<T>): Mono<T> = bodyToMono(type.java)

/**
 * Extension for [ServerRequest.bodyToMono] providing a `bodyToMono<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerRequest.bodyToMono(): Mono<T> = bodyToMono(T::class.java)

/**
 * Extension for [ServerRequest.bodyToFlux] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ServerRequest.bodyToFlux(type: KClass<T>): Flux<T> = bodyToFlux(type.java)

/**
 * Extension for [ServerRequest.bodyToFlux] providing a `bodyToFlux<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerRequest.bodyToFlux(): Flux<T> = bodyToFlux(T::class.java)
