package org.springframework.web.reactive.function.client

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


/**
 * Extension for [ClientResponse.bodyToMono] providing a `bodyToMono<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.bodyToMono(): Mono<T> = bodyToMono(T::class.java)

/**
 * Extension for [ClientResponse.bodyToFlux] providing a `bodyToFlux<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.bodyToFlux(): Flux<T> = bodyToFlux(T::class.java)
