/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.rsocket.service

import io.rsocket.util.DefaultPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.messaging.rsocket.TestRSocket
import org.springframework.util.MimeTypeUtils.TEXT_PLAIN
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Kotlin tests for [RSocketServiceMethod].
 *
 * @author Dmitry Sulman
 * @author Sebastien Deleuze
 */
class RSocketServiceMethodKotlinTests {

	private lateinit var rsocket: TestRSocket

	private lateinit var proxyFactory: RSocketServiceProxyFactory

	@BeforeEach
	fun setUp() {
		rsocket = TestRSocket()
		val requester = RSocketRequester.wrap(rsocket, TEXT_PLAIN, TEXT_PLAIN, RSocketStrategies.create())
		proxyFactory = RSocketServiceProxyFactory.builder(requester).build()
	}

	@Test
	suspend fun fireAndForget() {
		val service = proxyFactory.createClient(SuspendingFunctionsService::class.java)

		val requestPayload = "request"
		service.fireAndForget(requestPayload)

		assertThat(rsocket.savedMethodName).isEqualTo("fireAndForget")
		assertThat(rsocket.savedPayload?.metadataUtf8).isEqualTo("ff")
		assertThat(rsocket.savedPayload?.dataUtf8).isEqualTo(requestPayload)
	}

	@Test
	suspend fun requestResponse() {
		val service = proxyFactory.createClient(SuspendingFunctionsService::class.java)

		val requestPayload = "request"
		val responsePayload = "response"
		rsocket.setPayloadMonoToReturn(Mono.just(DefaultPayload.create(responsePayload)))
		val response = service.requestResponse(requestPayload)

		assertThat(response).isEqualTo(responsePayload)
		assertThat(rsocket.savedMethodName).isEqualTo("requestResponse")
		assertThat(rsocket.savedPayload?.metadataUtf8).isEqualTo("rr")
		assertThat(rsocket.savedPayload?.dataUtf8).isEqualTo(requestPayload)
	}

	@Test
	suspend fun requestStream() {
		val service = proxyFactory.createClient(SuspendingFunctionsService::class.java)

		val requestPayload = "request"
		val responsePayload1 = "response1"
		val responsePayload2 = "response2"
		rsocket.setPayloadFluxToReturn(
			Flux.just(DefaultPayload.create(responsePayload1), DefaultPayload.create(responsePayload2)))
		val response = service.requestStream(requestPayload).toList()

		assertThat(response).containsExactly(responsePayload1, responsePayload2)
		assertThat(rsocket.savedMethodName).isEqualTo("requestStream")
		assertThat(rsocket.savedPayload?.metadataUtf8).isEqualTo("rs")
		assertThat(rsocket.savedPayload?.dataUtf8).isEqualTo(requestPayload)
	}

	@Test
	suspend fun nonSuspendingRequestStream() {
		val service = proxyFactory.createClient(NonSuspendingFunctionsService::class.java)

		val requestPayload = "request"
		val responsePayload1 = "response1"
		val responsePayload2 = "response2"
		rsocket.setPayloadFluxToReturn(
			Flux.just(DefaultPayload.create(responsePayload1), DefaultPayload.create(responsePayload2)))
		val response = service.requestStream(requestPayload).toList()

		assertThat(response).containsExactly(responsePayload1, responsePayload2)
		assertThat(rsocket.savedMethodName).isEqualTo("requestStream")
		assertThat(rsocket.savedPayload?.metadataUtf8).isEqualTo("rs")
		assertThat(rsocket.savedPayload?.dataUtf8).isEqualTo(requestPayload)
	}

	@Test
	suspend fun requestChannel() {
		val service = proxyFactory.createClient(SuspendingFunctionsService::class.java)

		val requestPayload1 = "request1"
		val requestPayload2 = "request2"
		val responsePayload1 = "response1"
		val responsePayload2 = "response2"
		rsocket.setPayloadFluxToReturn(
			Flux.just(DefaultPayload.create(responsePayload1), DefaultPayload.create(responsePayload2)))
		val response = service.requestChannel(flowOf(requestPayload1, requestPayload2)).toList()

		assertThat(response).containsExactly(responsePayload1, responsePayload2)
		assertThat(rsocket.savedMethodName).isEqualTo("requestChannel")

		val savedPayloads = rsocket.savedPayloadFlux
			?.asFlow()
			?.map { it.dataUtf8 }
			?.toList()
		assertThat(savedPayloads).containsExactly(requestPayload1, requestPayload2)
	}

	private interface SuspendingFunctionsService {

		@RSocketExchange("ff")
		suspend fun fireAndForget(input: String)

		@RSocketExchange("rr")
		suspend fun requestResponse(input: String): String

		@RSocketExchange("rs")
		suspend fun requestStream(input: String): Flow<String>

		@RSocketExchange("rc")
		suspend fun requestChannel(input: Flow<String>): Flow<String>
	}

	private interface NonSuspendingFunctionsService {

		@RSocketExchange("rs")
		fun requestStream(input: String): Flow<String>
	}

}