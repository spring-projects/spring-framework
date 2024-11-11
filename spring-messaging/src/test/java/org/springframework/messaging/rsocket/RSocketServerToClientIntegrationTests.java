/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.messaging.rsocket;

import java.time.Duration;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;

/**
 * Client-side handling of requests initiated from the server side.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
class RSocketServerToClientIntegrationTests {

	private static AnnotationConfigApplicationContext context;

	private static CloseableChannel server;


	@BeforeAll
	@SuppressWarnings("ConstantConditions")
	static void setupOnce() {
		context = new AnnotationConfigApplicationContext(RSocketConfig.class);
		RSocketMessageHandler messageHandler = context.getBean(RSocketMessageHandler.class);
		SocketAcceptor responder = messageHandler.responder();

		server = RSocketServer.create(responder)
				.payloadDecoder(PayloadDecoder.ZERO_COPY)
				.bind(TcpServerTransport.create("localhost", 0))
				.block();
	}

	@AfterAll
	static void tearDownOnce() {
		server.dispose();
	}


	@Test
	void echo() {
		connectAndRunTest("echo");
	}

	@Test
	void echoAsync() {
		connectAndRunTest("echo-async");
	}

	@Test
	void echoStream() {
		connectAndRunTest("echo-stream");
	}

	@Test
	void echoChannel() {
		connectAndRunTest("echo-channel");
	}


	private static void connectAndRunTest(String connectionRoute) {
		context.getBean(ServerController.class).reset();

		RSocketStrategies strategies = context.getBean(RSocketStrategies.class);
		SocketAcceptor responder = RSocketMessageHandler.responder(strategies, new ClientHandler());

		RSocketRequester requester = null;
		try {
			requester = RSocketRequester.builder()
					.setupRoute(connectionRoute)
					.rsocketStrategies(strategies)
					.rsocketConnector(connector -> connector.acceptor(responder))
					.tcp("localhost", server.address().getPort());

			// Trigger connection establishment.
			requester.rsocketClient().source().block();

			context.getBean(ServerController.class).await(Duration.ofSeconds(5));
		}
		finally {
			if (requester != null) {
				requester.rsocketClient().dispose();
			}
		}
	}


	@Controller
	@SuppressWarnings("unused")
	static class ServerController {

		// Must be initialized by @Test method...
		volatile Sinks.Empty<Void> resultSink;


		void reset() {
			this.resultSink = Sinks.empty();
		}

		void await(Duration duration) {
			this.resultSink.asMono().block(duration);
		}


		@ConnectMapping("echo")
		void echo(RSocketRequester requester) {
			runTest(() -> {
				Flux<String> flux = Flux.range(1, 3).concatMap(i ->
						requester.route("echo").data("Hello " + i).retrieveMono(String.class));

				StepVerifier.create(flux)
						.expectNext("Hello 1")
						.expectNext("Hello 2")
						.expectNext("Hello 3")
						.expectComplete()
						.verify(Duration.ofSeconds(5));
			});
		}

		@ConnectMapping("echo-async")
		void echoAsync(RSocketRequester requester) {
			runTest(() -> {
				Flux<String> flux = Flux.range(1, 3).concatMap(i ->
						requester.route("echo-async").data("Hello " + i).retrieveMono(String.class));

				StepVerifier.create(flux)
						.expectNext("Hello 1 async")
						.expectNext("Hello 2 async")
						.expectNext("Hello 3 async")
						.expectComplete()
						.verify(Duration.ofSeconds(5));
			});
		}

		@ConnectMapping("echo-stream")
		void echoStream(RSocketRequester requester) {
			runTest(() -> {
				Flux<String> flux = requester.route("echo-stream").data("Hello").retrieveFlux(String.class);

				StepVerifier.create(flux)
						.expectNext("Hello 0")
						.expectNextCount(5)
						.expectNext("Hello 6")
						.expectNext("Hello 7")
						.thenCancel()
						.verify(Duration.ofSeconds(5));
			});
		}

		@ConnectMapping("echo-channel")
		void echoChannel(RSocketRequester requester) {
			runTest(() -> {
				Flux<String> flux = requester.route("echo-channel")
						.data(Flux.range(1, 10).map(i -> "Hello " + i), String.class)
						.retrieveFlux(String.class);

				StepVerifier.create(flux)
						.expectNext("Hello 1 async")
						.expectNextCount(7)
						.expectNext("Hello 9 async")
						.expectNext("Hello 10 async")
						.verifyComplete();
			});
		}

		private void runTest(Runnable testEcho) {
			Mono.fromRunnable(testEcho)
					.subscribeOn(Schedulers.boundedElastic()) // StepVerifier will block
					.subscribe(
							aVoid -> {},
							ex -> resultSink.tryEmitError(ex), // Ignore result: signals cannot compete
							() -> resultSink.tryEmitEmpty()
					);
		}

		@MessageMapping("fnf")
		void handleFireAndForget() {
		}
	}


	private static class ClientHandler {

		final Sinks.Many<String> fireForgetPayloads = Sinks.many().replay().all();

		@MessageMapping("receive")
		void receive(String payload) {
			this.fireForgetPayloads.emitNext(payload, Sinks.EmitFailureHandler.FAIL_FAST);
		}

		@MessageMapping("echo")
		String echo(String payload) {
			return payload;
		}

		@MessageMapping("echo-async")
		Mono<String> echoAsync(String payload) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> payload + " async");
		}

		@MessageMapping("echo-stream")
		Flux<String> echoStream(String payload) {
			return Flux.interval(Duration.ofMillis(10)).map(aLong -> payload + " " + aLong);
		}

		@MessageMapping("echo-channel")
		Flux<String> echoChannel(Flux<String> payloads) {
			return payloads.delayElements(Duration.ofMillis(10)).map(payload -> payload + " async");
		}
	}


	@Configuration
	static class RSocketConfig {

		@Bean
		ServerController serverController() {
			return new ServerController();
		}

		@Bean
		RSocketMessageHandler serverMessageHandler() {
			RSocketMessageHandler handler = new RSocketMessageHandler();
			handler.setRSocketStrategies(rsocketStrategies());
			return handler;
		}

		@Bean
		RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.create();
		}
	}

}
