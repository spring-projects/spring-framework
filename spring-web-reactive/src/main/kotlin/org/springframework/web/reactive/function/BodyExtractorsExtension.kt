package org.springframework.web.reactive.function

import org.springframework.http.ReactiveHttpInputMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

/**
 * Extension for [BodyExtactors] providing [KClass] based API and avoiding specifying
 * a class parameter when possible thanks to Kotlin reified type parameters.
 *
 * @since 5.0
 */
object BodyExtractorsExtension {

	/**
	 * @see BodyExtactors.toMono
	 */
	inline fun <reified T : Any> toMono() : BodyExtractor<Mono<T>, ReactiveHttpInputMessage> =
			BodyExtractors.toMono(T::class.java)

	/**
	 * @see BodyExtactors.toMono
	 */
	fun <T : Any> toMono(elementClass: KClass<T>) : BodyExtractor<Mono<T>, ReactiveHttpInputMessage> =
			BodyExtractors.toMono(elementClass.java)

	/**
	 * @see BodyExtactors.toFlux
	 */
	inline fun <reified T : Any> toFlux() : BodyExtractor<Flux<T>, ReactiveHttpInputMessage> =
			BodyExtractors.toFlux(T::class.java)

	/**
	 * @see BodyExtactors.toFlux
	 */
	fun <T : Any> toFlux(elementClass: KClass<T>) : BodyExtractor<Flux<T>, ReactiveHttpInputMessage> =
			BodyExtractors.toFlux(elementClass.java)
}
