package org.springframework.web.reactive.function

import org.springframework.http.ReactiveHttpInputMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.reflect.KClass

/**
 * Function for providing a `toMono()` alternative to `BodyExtractors.toMono(Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> toMono(): BodyExtractor<Mono<T>, ReactiveHttpInputMessage> =
		BodyExtractors.toMono(T::class.java)

/**
 * Function for providing a `toMono(Foo::class)` alternative to `BodyExtractors.toMono(Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> toMono(elementClass: KClass<T>): BodyExtractor<Mono<T>, ReactiveHttpInputMessage> =
		BodyExtractors.toMono(elementClass.java)

/**
 * Function for providing a `toFlux()` alternative to `BodyExtractors.toFlux(Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> toFlux(): BodyExtractor<Flux<T>, ReactiveHttpInputMessage> =
		BodyExtractors.toFlux(T::class.java)

/**
 * Function for providing a `toFlux(Foo::class)` alternative to `BodyExtractors.toFlux(Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
fun <T : Any> toFlux(elementClass: KClass<T>): BodyExtractor<Flux<T>, ReactiveHttpInputMessage> =
		BodyExtractors.toFlux(elementClass.java)
