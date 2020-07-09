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

package org.springframework.messaging.rsocket;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Server-side handling of RSocket requests.
 *
 * @author Rossen Stoyanchev
 */
public class RSocketClientToServerIntegrationTests {

	private static final MimeType FOO_MIME_TYPE = MimeTypeUtils.parseMimeType("messaging/x.foo");


	private static AnnotationConfigApplicationContext context;

	private static CloseableChannel server;

	private static CountingInterceptor interceptor = new CountingInterceptor();

	private static RSocketRequester requester;


	@BeforeAll
	@SuppressWarnings("ConstantConditions")
	public static void setupOnce() {

		MimeType metadataMimeType = MimeTypeUtils.parseMimeType(
				WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());

		context = new AnnotationConfigApplicationContext(ServerConfig.class);
		RSocketMessageHandler messageHandler = context.getBean(RSocketMessageHandler.class);
		SocketAcceptor responder = messageHandler.responder();

		server = RSocketServer.create(responder)
				.interceptors(registry -> registry.forResponder(interceptor))
				.payloadDecoder(PayloadDecoder.ZERO_COPY)
				.bind(TcpServerTransport.create("localhost", 7000))
				.block();

		requester = RSocketRequester.builder()
				.metadataMimeType(metadataMimeType)
				.rsocketStrategies(context.getBean(RSocketStrategies.class))
				.connectTcp("localhost", 7000)
				.block();
	}

	@AfterAll
	public static void tearDownOnce() {
		requester.rsocket().dispose();
		server.dispose();
	}


	@Test
	public void fireAndForget() {
		Flux.range(1, 3)
				.concatMap(i -> requester.route("receive").data("Hello " + i).send())
				.blockLast();

		StepVerifier.create(context.getBean(ServerController.class).fireForgetPayloads.asFlux())
				.expectNext("Hello 1")
				.expectNext("Hello 2")
				.expectNext("Hello 3")
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5));

		assertThat(interceptor.getFireAndForgetCount())
				.as("Fire and forget requests did not actually complete handling on the server side")
				.isEqualTo(3);
	}

	@Test
	public void echo() {
		Flux<String> result = Flux.range(1, 3).concatMap(i ->
				requester.route("echo").data("Hello " + i).retrieveMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello 1").expectNext("Hello 2").expectNext("Hello 3")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void echoAsync() {
		Flux<String> result = Flux.range(1, 3).concatMap(i ->
				requester.route("echo-async").data("Hello " + i).retrieveMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNext("Hello 2 async").expectNext("Hello 3 async")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void echoStream() {
		Flux<String> result = requester.route("echo-stream").data("Hello").retrieveFlux(String.class);

		StepVerifier.create(result)
				.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
				.thenCancel()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void echoChannel() {
		Flux<String> result = requester.route("echo-channel")
				.data(Flux.range(1, 10).map(i -> "Hello " + i), String.class)
				.retrieveFlux(String.class);

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNextCount(8).expectNext("Hello 10 async")
				.thenCancel()  // https://github.com/rsocket/rsocket-java/issues/613
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void metadataPush() {
		Flux.just("bar", "baz")
				.concatMap(s -> requester.route("foo-updates").metadata(s, FOO_MIME_TYPE).sendMetadata())
				.blockLast();

		StepVerifier.create(context.getBean(ServerController.class).metadataPushPayloads.asFlux())
				.expectNext("bar")
				.expectNext("baz")
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5));

		assertThat(interceptor.getMetadataPushCount())
				.as("Metadata pushes did not actually complete handling on the server side")
				.isEqualTo(2);
	}

	@Test
	public void voidReturnValue() {
		Mono<String> result = requester.route("void-return-value").data("Hello").retrieveMono(String.class);
		StepVerifier.create(result).expectComplete().verify(Duration.ofSeconds(5));
	}

	@Test
	public void voidReturnValueFromExceptionHandler() {
		Mono<String> result = requester.route("void-return-value").data("bad").retrieveMono(String.class);
		StepVerifier.create(result).expectComplete().verify(Duration.ofSeconds(5));
	}

	@Test
	public void handleWithThrownException() {
		Mono<String> result = requester.route("thrown-exception").data("a").retrieveMono(String.class);
		StepVerifier.create(result)
				.expectNext("Invalid input error handled")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void handleWithErrorSignal() {
		Mono<String> result = requester.route("error-signal").data("a").retrieveMono(String.class);
		StepVerifier.create(result)
				.expectNext("Invalid input error handled")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void noMatchingRoute() {
		Mono<String> result = requester.route("invalid").data("anything").retrieveMono(String.class);
		StepVerifier.create(result)
				.expectErrorMessage("No handler for destination 'invalid'")
				.verify(Duration.ofSeconds(5));
	}


	@Controller
	static class ServerController {

		final Sinks.StandaloneFluxSink<String> fireForgetPayloads = Sinks.replayAll();

		final Sinks.StandaloneFluxSink<String> metadataPushPayloads = Sinks.replayAll();


		@MessageMapping("receive")
		void receive(String payload) {
			this.fireForgetPayloads.next(payload);
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

		@MessageMapping("thrown-exception")
		Mono<String> handleAndThrow(String payload) {
			throw new IllegalArgumentException("Invalid input error");
		}

		@MessageMapping("error-signal")
		Mono<String> handleAndReturnError(String payload) {
			return Mono.error(new IllegalArgumentException("Invalid input error"));
		}

		@MessageMapping("void-return-value")
		Mono<Void> voidReturnValue(String payload) {
			return !payload.equals("bad") ?
					Mono.delay(Duration.ofMillis(10)).then(Mono.empty()) :
					Mono.error(new IllegalStateException("bad"));
		}

		@ConnectMapping("foo-updates")
		public void handleMetadata(@Header("foo") String foo) {
			this.metadataPushPayloads.next(foo);
		}

		@MessageExceptionHandler
		Mono<String> handleException(IllegalArgumentException ex) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> ex.getMessage() + " handled");
		}

		@MessageExceptionHandler
		Mono<Void> handleExceptionWithVoidReturnValue(IllegalStateException ex) {
			return Mono.delay(Duration.ofMillis(10)).then(Mono.empty());
		}
	}


	@Configuration
	static class ServerConfig {

		@Bean
		public ServerController controller() {
			return new ServerController();
		}

		@Bean
		public RSocketMessageHandler messageHandler() {
			RSocketMessageHandler handler = new RSocketMessageHandler();
			handler.setRSocketStrategies(rsocketStrategies());
			return handler;
		}

		@Bean
		public RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.builder()
					.metadataExtractorRegistry(registry ->
							registry.metadataToExtract(FOO_MIME_TYPE, String.class, "foo"))
					.build();
		}
	}


	private static class CountingInterceptor implements RSocket, RSocketInterceptor {

		private RSocket delegate;

		private final AtomicInteger fireAndForgetCount = new AtomicInteger(0);

		private final AtomicInteger metadataPushCount = new AtomicInteger(0);


		public int getFireAndForgetCount() {
			return this.fireAndForgetCount.get();
		}

		public int getMetadataPushCount() {
			return this.metadataPushCount.get();
		}

		@Override
		public RSocket apply(RSocket rsocket) {
			Assert.isNull(this.delegate, "Unexpected RSocket connection");
			this.delegate = rsocket;
			return this;
		}

		@Override
		public Mono<Void> fireAndForget(Payload payload) {
			return this.delegate.fireAndForget(payload)
					.doOnSuccess(aVoid -> this.fireAndForgetCount.incrementAndGet());
		}

		@Override
		public Mono<Void> metadataPush(Payload payload) {
			return this.delegate.metadataPush(payload)
					.doOnSuccess(aVoid -> this.metadataPushCount.incrementAndGet());
		}

		@Override
		public Mono<Payload> requestResponse(Payload payload) {
			return this.delegate.requestResponse(payload);
		}

		@Override
		public Flux<Payload> requestStream(Payload payload) {
			return this.delegate.requestStream(payload);
		}

		@Override
		public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
			return this.delegate.requestChannel(payloads);
		}
	}
}
