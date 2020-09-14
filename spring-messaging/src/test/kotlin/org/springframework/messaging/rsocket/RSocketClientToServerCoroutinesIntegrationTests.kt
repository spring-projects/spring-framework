/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.rsocket

import io.netty.buffer.PooledByteBufAllocator
import io.rsocket.core.RSocketServer
import io.rsocket.frame.decoder.PayloadDecoder
import io.rsocket.transport.netty.server.CloseableChannel
import io.rsocket.transport.netty.server.TcpServerTransport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.codec.CharSequenceEncoder
import org.springframework.core.codec.StringDecoder
import org.springframework.core.io.buffer.NettyDataBufferFactory
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import java.time.Duration

/**
 * Coroutines server-side handling of RSocket requests.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
class RSocketClientToServerCoroutinesIntegrationTests {

	@Test
	fun fireAndForget() {
		Flux.range(1, 3)
				.concatMap { requester.route("receive").data("Hello $it").send() }
				.blockLast()
		StepVerifier.create(context.getBean(ServerController::class.java).fireForgetPayloads.asFlux())
				.expectNext("Hello 1")
				.expectNext("Hello 2")
				.expectNext("Hello 3")
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5))
	}

	@Test
	fun fireAndForgetAsync() {
		Flux.range(1, 3)
				.concatMap { i: Int -> requester.route("receive-async").data("Hello $i").send() }
				.blockLast()
		StepVerifier.create(context.getBean(ServerController::class.java).fireForgetPayloads.asFlux())
				.expectNext("Hello 1")
				.expectNext("Hello 2")
				.expectNext("Hello 3")
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5))
	}

	@Test
	fun echoAsync() {
		val result = Flux.range(1, 3).concatMap { i -> requester.route("echo-async").data("Hello " + i!!).retrieveMono(String::class.java) }

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNext("Hello 2 async").expectNext("Hello 3 async")
				.expectComplete()
				.verify(Duration.ofSeconds(5))
	}

	@Test
	fun echoStream() {
		val result = requester.route("echo-stream").data("Hello").retrieveFlux(String::class.java)

		StepVerifier.create(result)
				.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
				.thenCancel()
				.verify(Duration.ofSeconds(5))
	}

	@Test
	fun echoStreamAsync() {
		val result = requester.route("echo-stream-async").data("Hello").retrieveFlux(String::class.java)

		StepVerifier.create(result)
				.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
				.thenCancel()
				.verify(Duration.ofSeconds(5))
	}

	@Test
	fun echoChannel() {
		val result = requester.route("echo-channel")
				.data(Flux.range(1, 10).map { i -> "Hello " + i!! }, String::class.java)
				.retrieveFlux(String::class.java)

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNextCount(8).expectNext("Hello 10 async")
				.thenCancel()  // https://github.com/rsocket/rsocket-java/issues/613
				.verify(Duration.ofSeconds(5))
	}

	@Test
	fun unitReturnValue() {
		val result = requester.route("unit-return-value").data("Hello").retrieveMono(String::class.java)
		StepVerifier.create(result).expectComplete().verify(Duration.ofSeconds(5))
	}

	@Test
	fun unitReturnValueFromExceptionHandler() {
		val result = requester.route("unit-return-value").data("bad").retrieveMono(String::class.java)
		StepVerifier.create(result).expectComplete().verify(Duration.ofSeconds(5))
	}

	@Test
	fun handleWithThrownException() {
		val result = requester.route("thrown-exception").data("a").retrieveMono(String::class.java)
		StepVerifier.create(result)
				.expectNext("Invalid input error handled")
				.expectComplete()
				.verify(Duration.ofSeconds(5))
	}

	@Controller
	class ServerController {

		val fireForgetPayloads = Sinks.many().replay().all<String>()

		@MessageMapping("receive")
		fun receive(payload: String) {
			fireForgetPayloads.tryEmitNext(payload)
		}

		@MessageMapping("receive-async")
		suspend fun receiveAsync(payload: String) {
			delay(10)
			fireForgetPayloads.tryEmitNext(payload)
		}

		@MessageMapping("echo-async")
		suspend fun echoAsync(payload: String): String {
			delay(10)
			return "$payload async"
		}

		@MessageMapping("echo-stream")
		fun echoStream(payload: String): Flow<String> {
			var i = 0
			return flow {
				while(true) {
					delay(10)
					emit("$payload ${i++}")
				}
			}
		}

		@MessageMapping("echo-stream-async")
		suspend fun echoStreamAsync(payload: String): Flow<String> {
			delay(10)
			var i = 0
			return flow {
				while(true) {
					delay(10)
					emit("$payload ${i++}")
				}
			}
		}

		@MessageMapping("echo-channel")
		fun echoChannel(payloads: Flow<String>) = payloads.map {
			delay(10)
			"$it async"
		}

		@Suppress("UNUSED_PARAMETER")
		@MessageMapping("thrown-exception")
		suspend fun handleAndThrow(payload: String): String {
			delay(10)
			throw IllegalArgumentException("Invalid input error")
		}

		@MessageMapping("unit-return-value")
		suspend fun unitReturnValue(payload: String) =
				if (payload != "bad") delay(10) else throw IllegalStateException("bad")

		@MessageExceptionHandler
		suspend fun handleException(ex: IllegalArgumentException): String {
			delay(10)
			return  "${ex.message} handled"
		}

		@Suppress("UNUSED_PARAMETER")
		@MessageExceptionHandler
		suspend fun handleExceptionWithVoidReturnValue(ex: IllegalStateException) {
			delay(10)
		}
	}


	@Configuration
	open class ServerConfig {

		@Bean
		open fun controller(): ServerController {
			return ServerController()
		}

		@Bean
		open fun messageHandler(): RSocketMessageHandler {
			val handler = RSocketMessageHandler()
			handler.rSocketStrategies = rsocketStrategies()
			return handler
		}

		@Bean
		open fun rsocketStrategies(): RSocketStrategies {
			return RSocketStrategies.builder()
					.decoder(StringDecoder.allMimeTypes())
					.encoder(CharSequenceEncoder.allMimeTypes())
					.dataBufferFactory(NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT))
					.build()
		}
	}

	companion object {

		private lateinit var context: AnnotationConfigApplicationContext

		private lateinit var server: CloseableChannel

		private lateinit var requester: RSocketRequester


		@BeforeAll
		@JvmStatic
		fun setupOnce() {
			context = AnnotationConfigApplicationContext(ServerConfig::class.java)

			server = RSocketServer.create(context.getBean(RSocketMessageHandler::class.java).responder())
					.payloadDecoder(PayloadDecoder.ZERO_COPY)
					.bind(TcpServerTransport.create("localhost", 7000))
					.block()!!

			requester = RSocketRequester.builder()
					.rsocketConnector { connector -> connector.payloadDecoder(PayloadDecoder.ZERO_COPY) }
					.rsocketStrategies(context.getBean(RSocketStrategies::class.java))
					.tcp("localhost", 7000)
		}

		@AfterAll
		@JvmStatic
		fun tearDownOnce() {
			requester.rsocketClient().dispose()
			server.dispose()
		}
	}

}
