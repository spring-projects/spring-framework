package org.springframework.web.reactive.function.server

import kotlin.reflect.KClass


/**
 * Extension for [ServerRequest.bodyToMono] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ServerRequest.bodyToMono(type: KClass<T>) = bodyToMono(type.java)

/**
 * Extension for [ServerRequest.bodyToFlux] providing a [KClass] based variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> ServerRequest.bodyToFlux(type: KClass<T>) = bodyToFlux(type.java)
