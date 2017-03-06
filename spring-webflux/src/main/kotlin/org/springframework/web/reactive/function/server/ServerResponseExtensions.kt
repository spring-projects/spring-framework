package org.springframework.web.reactive.function.server

import org.reactivestreams.Publisher

/**
 * Extension for [ServerResponse.BodyBuilder.body] providing a `body(Publisher<T>)` variant.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any> ServerResponse.BodyBuilder.body(publisher: Publisher<T>) = body(publisher, T::class.java)