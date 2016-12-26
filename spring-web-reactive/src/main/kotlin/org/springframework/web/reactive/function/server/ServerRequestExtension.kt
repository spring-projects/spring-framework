package org.springframework.web.reactive.function.server

import kotlin.reflect.KClass

/**
 * Extension for [ServerRequest] providing [KClass] based API.
 *
 * @since 5.0
 */
object ServerRequestExtension {

	/**
	 * @see ServerRequest.bodyToMono
	 */
	fun <T : Any> ServerRequest.bodyToMono(type: KClass<T>) = bodyToMono(type.java)

	/**
	 * @see ServerRequest.bodyToFlux
	 */
	fun <T : Any> ServerRequest.bodyToFlux(type: KClass<T>) = bodyToFlux(type.java)
}
