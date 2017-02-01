package org.springframework.web.reactive.function.client

import org.reactivestreams.Publisher

/**
 * Extension for [WebClient.HeaderSpec.exchange] providing a variant without explicit class
 * parameter thanks to Kotlin reified type parameters.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Any, S : Publisher<T>> WebClient.HeaderSpec.exchange(publisher: S) =
        exchange(publisher, T::class.java)
