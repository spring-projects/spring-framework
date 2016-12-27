package org.springframework.web.reactive.function.client

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

/**
 * Extension for [ClientResponse] providing [KClass] based API.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
object ClientResponseExtension {

	/**
	 * @see ClientResponse.bodyToFlux
	 */
	fun <T : Any> ClientResponse.bodyToFlux(type: KClass<T>) : Flux<T> = bodyToFlux(type.java)

	/**
	 * @see ClientResponse.bodyToMono
	 */
	fun <T : Any> ClientResponse.bodyToMono(type: KClass<T>) : Mono<T> = bodyToMono(type.java)
}
