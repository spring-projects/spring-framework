package org.springframework.web.reactive.function

import org.reactivestreams.Publisher
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.server.reactive.ServerHttpResponse

/**
 * Function for providing a `fromPublisher(publisher)` alternative to `BodyInserters.fromPublisher(publisher, Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Publisher<S>, reified S : Any> fromPublisher(publisher: T) : BodyInserter<T, ReactiveHttpOutputMessage> =
		BodyInserters.fromPublisher(publisher, S::class.java)

/**
 * Function for providing a `fromServerSentEvents(publisher)` alternative to `BodyInserters.fromServerSentEvents(publisher, Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Publisher<S>, reified S : Any> fromServerSentEvents(publisher: T) : BodyInserter<T, ServerHttpResponse> =
		BodyInserters.fromServerSentEvents(publisher, S::class.java)
