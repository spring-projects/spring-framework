package org.springframework.web.reactive.function.client

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass


/**
 * Extension for [ClientResponse.bodyToMono] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ClientResponse.bodyToMono(type: KClass<T>) : Mono<T> = bodyToMono(type.java)

/**
 * Extension for [ClientResponse.bodyToMono] providing a `bodyToMono<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.bodyToMono() = bodyToMono(T::class.java)


/**
 * Extension for [ClientResponse.bodyToFlux] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ClientResponse.bodyToFlux(type: KClass<T>) : Flux<T> = bodyToFlux(type.java)

/**
 * Extension for [ClientResponse.bodyToFlux] providing a `bodyToFlux<Foo>()` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ClientResponse.bodyToFlux() = bodyToFlux(T::class.java)



