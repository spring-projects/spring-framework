package org.springframework.web.reactive.function

import org.springframework.http.ReactiveHttpInputMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Function for providing a `bodyToMono()` alternative to `BodyExtractors.toMono(Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see [KT-11968](https://youtrack.jetbrains.com/issue/KT-11968)
 */
inline fun <reified T : Any> bodyToMono(): BodyExtractor<Mono<T>, ReactiveHttpInputMessage> =
		BodyExtractors.toMono(T::class.java)

/**
 * Function for providing a `bodyToFlux()` alternative to `BodyExtractors.toFlux(Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see [KT-11968](https://youtrack.jetbrains.com/issue/KT-11968)
 */
inline fun <reified T : Any> bodyToFlux(): BodyExtractor<Flux<T>, ReactiveHttpInputMessage> =
		BodyExtractors.toFlux(T::class.java)

