/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.rsocket;

import java.time.Duration;
import java.util.Collections;

import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.Frame;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * Client-side handling of requests initiated from the server side.
 *
 *  @author Rossen Stoyanchev
 */
public class RSocketServerToClientIntegrationTests {

	private static AnnotationConfigApplicationContext context;

	private static Closeable server;


	@BeforeClass
	@SuppressWarnings("ConstantConditions")
	public static void setupOnce() {
		context = new AnnotationConfigApplicationContext(RSocketConfig.class);

		server = RSocketFactory.receive()
				.frameDecoder(Frame::retain)  // as per https://github.com/rsocket/rsocket-java#zero-copy
				.acceptor(context.getBean("serverAcceptor", MessageHandlerAcceptor.class))
				.transport(TcpServerTransport.create("localhost", 7000))
				.start()
				.block();
	}

	@AfterClass
	public static void tearDownOnce() {
		server.dispose();
	}


	@Test
	public void echo() {
		connectAndVerify("connect.echo");
	}

	@Test
	public void echoAsync() {
		connectAndVerify("connect.echo-async");
	}

	@Test
	public void echoStream() {
		connectAndVerify("connect.echo-stream");
	}

	@Test
	public void echoChannel() {
		connectAndVerify("connect.echo-channel");
	}


	private static void connectAndVerify(String destination) {

		ServerController serverController = context.getBean(ServerController.class);
		serverController.reset();

		RSocket rsocket = null;
		try {
			rsocket = RSocketFactory.connect()
					.setupPayload(DefaultPayload.create("", destination))
					.dataMimeType("text/plain")
					.frameDecoder(Frame::retain)  // as per https://github.com/rsocket/rsocket-java#zero-copy
					.acceptor(context.getBean("clientAcceptor", MessageHandlerAcceptor.class))
					.transport(TcpClientTransport.create("localhost", 7000))
					.start()
					.block();

			serverController.await(Duration.ofSeconds(5));
		}
		finally {
			if (rsocket != null) {
				rsocket.dispose();
			}
		}
	}


	@Controller
	@SuppressWarnings({"unused", "NullableProblems"})
	static class ServerController {

		// Must be initialized by @Test method...
		volatile MonoProcessor<Void> result;


		public void reset() {
			this.result = MonoProcessor.create();
		}

		public void await(Duration duration) {
			this.result.block(duration);
		}


		@MessageMapping("connect.echo")
		void echo(RSocketRequester requester) {
			runTest(() -> {
				Flux<String> flux = Flux.range(1, 3).concatMap(i ->
						requester.route("echo").data("Hello " + i).retrieveMono(String.class));

				StepVerifier.create(flux)
						.expectNext("Hello 1")
						.expectNext("Hello 2")
						.expectNext("Hello 3")
						.verifyComplete();
			});
		}

		@MessageMapping("connect.echo-async")
		void echoAsync(RSocketRequester requester) {
			runTest(() -> {
				Flux<String> flux = Flux.range(1, 3).concatMap(i ->
						requester.route("echo-async").data("Hello " + i).retrieveMono(String.class));

				StepVerifier.create(flux)
						.expectNext("Hello 1 async")
						.expectNext("Hello 2 async")
						.expectNext("Hello 3 async")
						.verifyComplete();
			});
		}

		@MessageMapping("connect.echo-stream")
		void echoStream(RSocketRequester requester) {
			runTest(() -> {
				Flux<String> flux = requester.route("echo-stream").data("Hello").retrieveFlux(String.class);

				StepVerifier.create(flux)
						.expectNext("Hello 0")
						.expectNextCount(5)
						.expectNext("Hello 6")
						.expectNext("Hello 7")
						.thenCancel()
						.verify();
			});
		}

		@MessageMapping("connect.echo-channel")
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
					.doOnError(ex -> result.onError(ex))
					.doOnSuccess(o -> result.onComplete())
					.subscribeOn(Schedulers.elastic()) // StepVerifier will block
					.subscribe();
		}
	}


	private static class ClientHandler {

		final ReplayProcessor<String> fireForgetPayloads = ReplayProcessor.create();

		@MessageMapping("receive")
		void receive(String payload) {
			this.fireForgetPayloads.onNext(payload);
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
		public ClientHandler clientHandler() {
			return new ClientHandler();
		}

		@Bean
		public ServerController serverController() {
			return new ServerController();
		}

		@Bean
		public MessageHandlerAcceptor clientAcceptor() {
			MessageHandlerAcceptor acceptor = new MessageHandlerAcceptor();
			acceptor.setHandlers(Collections.singletonList(clientHandler()));
			acceptor.setAutoDetectDisabled();
			acceptor.setRSocketStrategies(rsocketStrategies());
			return acceptor;
		}

		@Bean
		public MessageHandlerAcceptor serverAcceptor() {
			MessageHandlerAcceptor handler = new MessageHandlerAcceptor();
			handler.setRSocketStrategies(rsocketStrategies());
			return handler;
		}

		@Bean
		public RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.builder()
					.decoder(StringDecoder.allMimeTypes())
					.encoder(CharSequenceEncoder.allMimeTypes())
					.dataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT))
					.build();
		}
	}

}
