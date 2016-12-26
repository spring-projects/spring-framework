package org.springframework.web.reactive.function

import org.reactivestreams.Publisher
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.server.reactive.ServerHttpResponse

/**
 * Extension for [BodyInserters] providing [KClass] based API and avoiding specifying
 * a class parameter when possible thanks to Kotlin reified type parameters.
 *
 * @since 5.0
 */
object BodyInsertersExtension {

	/**
	 * @see BodyInserters.fromPublisher
	 */
	inline fun <reified T : Publisher<S>, reified S : Any> fromPublisher(publisher: T) : BodyInserter<T, ReactiveHttpOutputMessage> =
			BodyInserters.fromPublisher(publisher, S::class.java)

	/**
	 * @see BodyInserters.fromServerSentEvents
	 */
	inline fun <reified T : Publisher<S>, reified S : Any> fromServerSentEvents(publisher: T) : BodyInserter<T, ServerHttpResponse> =
			BodyInserters.fromServerSentEvents(publisher, S::class.java)
}
