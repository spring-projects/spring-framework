package org.springframework.web.reactive.function

import org.reactivestreams.Publisher
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.server.reactive.ServerHttpResponse

/**
 * Function for providing a `bodyFromPublisher(publisher)` alternative to `BodyInserters.fromPublisher(publisher, Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Publisher<S>, reified S : Any> bodyFromPublisher(publisher: T): BodyInserter<T, ReactiveHttpOutputMessage> =
		BodyInserters.fromPublisher(publisher, S::class.java)

/**
 * Function for providing a `bodyFromServerSentEvents(publisher)` alternative to `BodyInserters.fromServerSentEvents(publisher, Foo::class.java)`.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 */
inline fun <reified T : Publisher<S>, reified S : Any> bodyFromServerSentEvents(publisher: T): BodyInserter<T, ServerHttpResponse> =
		BodyInserters.fromServerSentEvents(publisher, S::class.java)
