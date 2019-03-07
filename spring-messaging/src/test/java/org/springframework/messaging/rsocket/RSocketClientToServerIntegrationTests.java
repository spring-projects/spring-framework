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

import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Frame;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;

import static org.junit.Assert.*;

/**
 * Server-side handling of RSocket requests.
 *
 * @author Rossen Stoyanchev
 */
public class RSocketClientToServerIntegrationTests {

	private static AnnotationConfigApplicationContext context;

	private static CloseableChannel server;

	private static FireAndForgetCountingInterceptor interceptor = new FireAndForgetCountingInterceptor();

	private static RSocket client;

	private static RSocketRequester requester;


	@BeforeClass
	@SuppressWarnings("ConstantConditions")
	public static void setupOnce() {
		context = new AnnotationConfigApplicationContext(ServerConfig.class);

		server = RSocketFactory.receive()
				.addServerPlugin(interceptor)
				.frameDecoder(Frame::retain)  // as per https://github.com/rsocket/rsocket-java#zero-copy
				.acceptor(context.getBean(MessageHandlerAcceptor.class))
				.transport(TcpServerTransport.create("localhost", 7000))
				.start()
				.block();

		client = RSocketFactory.connect()
				.dataMimeType(MimeTypeUtils.TEXT_PLAIN_VALUE)
				.frameDecoder(Frame::retain)  // as per https://github.com/rsocket/rsocket-java#zero-copy
				.transport(TcpClientTransport.create("localhost", 7000))
				.start()
				.block();

		requester = RSocketRequester.create(
				client, MimeTypeUtils.TEXT_PLAIN, context.getBean(RSocketStrategies.class));
	}

	@AfterClass
	public static void tearDownOnce() {
		client.dispose();
		server.dispose();
	}


	@Test
	public void fireAndForget() {
		Flux.range(1, 3)
				.concatMap(i -> requester.route("receive").data("Hello " + i).send())
				.blockLast();

		StepVerifier.create(context.getBean(ServerController.class).fireForgetPayloads)
				.expectNext("Hello 1")
				.expectNext("Hello 2")
				.expectNext("Hello 3")
				.thenCancel()
				.verify(Duration.ofSeconds(5));

		assertEquals(1, interceptor.getRSocketCount());
		assertEquals("Fire and forget requests did not actually complete handling on the server side",
				3, interceptor.getFireAndForgetCount(0));
	}

	@Test
	public void echo() {
		Flux<String> result = Flux.range(1, 3).concatMap(i ->
				requester.route("echo").data("Hello " + i).retrieveMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello 1").expectNext("Hello 2").expectNext("Hello 3")
				.verifyComplete();
	}

	@Test
	public void echoAsync() {
		Flux<String> result = Flux.range(1, 3).concatMap(i ->
				requester.route("echo-async").data("Hello " + i).retrieveMono(String.class));

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNext("Hello 2 async").expectNext("Hello 3 async")
				.verifyComplete();
	}

	@Test
	public void echoStream() {
		Flux<String> result = requester.route("echo-stream").data("Hello").retrieveFlux(String.class);

		StepVerifier.create(result)
				.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
				.thenCancel()
				.verify();
	}

	@Test
	public void echoChannel() {
		Flux<String> result = requester.route("echo-channel")
				.data(Flux.range(1, 10).map(i -> "Hello " + i), String.class)
				.retrieveFlux(String.class);

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNextCount(8).expectNext("Hello 10 async")
				.verifyComplete();
	}

	@Test
	public void voidReturnValue() {
		Flux<String> result = requester.route("void-return-value").data("Hello").retrieveFlux(String.class);
		StepVerifier.create(result).verifyComplete();
	}

	@Test
	public void voidReturnValueFromExceptionHandler() {
		Flux<String> result = requester.route("void-return-value").data("bad").retrieveFlux(String.class);
		StepVerifier.create(result).verifyComplete();
	}

	@Test
	public void handleWithThrownException() {
		Mono<String> result = requester.route("thrown-exception").data("a").retrieveMono(String.class);
		StepVerifier.create(result).expectNext("Invalid input error handled").verifyComplete();
	}

	@Test
	public void handleWithErrorSignal() {
		Mono<String> result = requester.route("error-signal").data("a").retrieveMono(String.class);
		StepVerifier.create(result).expectNext("Invalid input error handled").verifyComplete();
	}

	@Test
	public void noMatchingRoute() {
		Mono<String> result = requester.route("invalid").data("anything").retrieveMono(String.class);
		StepVerifier.create(result).verifyErrorMessage("No handler for destination 'invalid'");
	}


	@Controller
	static class ServerController {

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
		public MessageHandlerAcceptor messageHandlerAcceptor() {
			MessageHandlerAcceptor acceptor = new MessageHandlerAcceptor();
			acceptor.setRSocketStrategies(rsocketStrategies());
			return acceptor;
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
