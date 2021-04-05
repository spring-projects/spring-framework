/*
 * Copyright 2002-2021 the original author or authors.
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

import io.netty.buffer.ByteBufAllocator;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.RSocketInterceptor;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.assertj.core.util.Lists;
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
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Server-side handling of RSocket requests with message mime type.
 *
 * @author Rossen Stoyanchev
 */
public class RSocketClientToServerContentTypeIntegrationTests {

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
				.tcp("localhost", 7000);
	}

	@AfterAll
	public static void tearDownOnce() {
		requester.rsocketClient().dispose();
		server.dispose();
	}

	@Test
	public void fireAndForgetWithMultiContentType() {
		String customMime = "text/*";
		assertThatThrownBy(()->
		Flux.range(1, 3)
				.concatMap(i -> requester.route("messageContentType")
						.metadata(Lists.newArrayList(customMime), MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()))
						.data("Hello " + i).send()).blockLast())
						.isInstanceOf(IllegalArgumentException.class).hasMessage("Message rsocket mime type should be type of String or ByteBuf");
	}

	@Test
	public void fireAndForgetWithEncodeContentType() {
		String customMime = "text/*";
		Flux.range(1, 3)
				.concatMap(i -> requester.route("messageContentType")
						.metadata(MimeTypeMetadataCodec.encode(ByteBufAllocator.DEFAULT, Lists.newArrayList(customMime)),
								MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()))
						.data("Hello " + i).send()).blockLast();

		StepVerifier.create(context.getBean(ServerController.class).fireForgetPayloads.asFlux())
				.expectNext("Hello 1")
				.expectNext("Hello 2")
				.expectNext("Hello 3")
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5));
		StepVerifier.create(context.getBean(ServerController.class).contentTypes.asFlux())
				.expectNext(customMime)
				.expectNext(customMime)
				.expectNext(customMime)
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5));

		assertThat(interceptor.getFireAndForgetCount())
				.as("Fire and forget requests did not actually complete handling on the server side")
				.isEqualTo(3);
		// clean
		interceptor.getFireForgeCount().set(0);
	}

	@Test
	public void fireAndForgetWithContentType() {
		String customMime = "text/*";
		Flux.range(1, 3)
				.concatMap(i -> requester.route("messageContentType")
						.metadata(customMime, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()))
						.data("Hello " + i).send()).blockLast();

		StepVerifier.create(context.getBean(ServerController.class).fireForgetPayloads.asFlux())
				.expectNext("Hello 1")
				.expectNext("Hello 2")
				.expectNext("Hello 3")
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5));
		StepVerifier.create(context.getBean(ServerController.class).contentTypes.asFlux())
				.expectNext(customMime)
				.expectNext(customMime)
				.expectNext(customMime)
				.thenAwait(Duration.ofMillis(50))
				.thenCancel()
				.verify(Duration.ofSeconds(5));

		assertThat(interceptor.getFireAndForgetCount())
				.as("Fire and forget requests did not actually complete handling on the server side")
				.isEqualTo(3);
		// clean
		interceptor.getFireForgeCount().set(0);
	}

	@Test
	public void echoRetrieveMono() {
		String customMime = "text/*";
		String accept = "text/*";
		Flux<String> result = Flux.range(1, 3).concatMap(i ->
				requester.route("echo")
						.metadata(customMime, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()))
						.metadata(Lists.newArrayList(accept),MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_ACCEPT_MIMETYPES.getString()))
						.data("Hello " + i).retrieveMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello 1").expectNext("Hello 2").expectNext("Hello 3")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void echoStream() {
		String customMime = "text/*";
		String accept = "text/plain";
		Flux<String> result = requester.route("echo-stream")
				.metadata(customMime, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()))
				.metadata(Lists.newArrayList(accept), MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_ACCEPT_MIMETYPES.getString()))
				.data("Hello").retrieveFlux(String.class);
		StepVerifier.create(result)
				.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
				.thenCancel()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	public void echoChannel() {
		String customMime = "text/*";
		String accept = "text/plain";
		Flux<String> result = requester.route("echo-channel")
				.metadata(customMime, MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_MIMETYPE.getString()))
				.metadata(Lists.newArrayList(accept), MimeType.valueOf(WellKnownMimeType.MESSAGE_RSOCKET_ACCEPT_MIMETYPES.getString()))
				.data(Flux.range(1, 10).map(i -> "Hello " + i), String.class)
				.retrieveFlux(String.class);

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNextCount(8).expectNext("Hello 10 async")
				.thenCancel()  // https://github.com/rsocket/rsocket-java/issues/613
				.verify(Duration.ofSeconds(5));
	}


	@Controller
	static class ServerController {

		final Sinks.Many<String> fireForgetPayloads = Sinks.many().replay().all();

		final Sinks.Many<String> contentTypes = Sinks.many().replay().all();

		@MessageMapping("messageContentType")
		void messageContentType(@Payload String payload, @Header(name = MessageHeaders.CONTENT_TYPE) MimeType contentType) {
			this.fireForgetPayloads.tryEmitNext(payload);
			this.contentTypes.tryEmitNext(contentType.toString());
		}

		@MessageMapping("echo")
		String echo(String payload) {
			return payload;
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

		@MessageMapping("thrown-exception-flux")
		Flux<String> handleAndThrowFlux(String payload) {
			throw new IllegalArgumentException("Invalid input error");
		}

		@MessageMapping("error-signal")
		Mono<String> handleAndReturnError(String payload) {
			return Mono.error(new IllegalArgumentException("Invalid input error"));
		}

		@MessageMapping("error-signal-flux")
		Flux<String> handleAndReturnErrorFLux(String payload) {
			return Flux.error(new IllegalArgumentException("Invalid input error"));
		}

		@MessageMapping("void-return-value")
		Mono<Void> voidReturnValue(String payload) {
			return !payload.equals("bad") ?
					Mono.delay(Duration.ofMillis(10)).then(Mono.empty()) :
					Mono.error(new IllegalStateException("bad"));
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

		private final AtomicInteger fireAndForgetCount = new AtomicInteger();

		private final AtomicInteger metadataPushCount = new AtomicInteger();


		public int getFireAndForgetCount() {
			return this.fireAndForgetCount.get();
		}

		public AtomicInteger getFireForgeCount(){
			return this.fireAndForgetCount;
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
		public Mono<Void> fireAndForget(io.rsocket.Payload payload) {
			return this.delegate.fireAndForget(payload)
					.doOnSuccess(aVoid -> this.fireAndForgetCount.incrementAndGet());
		}

		@Override
		public Mono<Void> metadataPush(io.rsocket.Payload payload) {
			return this.delegate.metadataPush(payload)
					.doOnSuccess(aVoid -> this.metadataPushCount.incrementAndGet());
		}

		@Override
		public Mono<io.rsocket.Payload> requestResponse(io.rsocket.Payload payload) {
			return this.delegate.requestResponse(payload);
		}

		@Override
		public Flux<io.rsocket.Payload> requestStream(io.rsocket.Payload payload) {
			return this.delegate.requestStream(payload);
		}

		@Override
		public Flux<io.rsocket.Payload> requestChannel(Publisher<io.rsocket.Payload> payloads) {
			return this.delegate.requestChannel(payloads);
		}
	}
}
