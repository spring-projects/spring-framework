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

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
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
import org.springframework.messaging.ReactiveMessageChannel;
import org.springframework.messaging.ReactiveSubscribableChannel;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.support.DefaultReactiveMessageChannel;
import org.springframework.stereotype.Controller;

import static org.junit.Assert.*;

/**
 * Server-side handling of RSocket requests.
 *
 * @author Rossen Stoyanchev
 */
public class RSocketClientToServerIntegrationTests {

	private static AnnotationConfigApplicationContext context;

	private static CloseableChannel serverChannel;

	private static FireAndForgetCountingInterceptor interceptor = new FireAndForgetCountingInterceptor();

	private static RSocket clientRsocket;


	@BeforeClass
	@SuppressWarnings("ConstantConditions")
	public static void setupOnce() {

		context = new AnnotationConfigApplicationContext(ServerConfig.class);

		MessagingAcceptor acceptor = new MessagingAcceptor(
				context.getBean("rsocketChannel", ReactiveMessageChannel.class));

		serverChannel = RSocketFactory.receive()
				.addServerPlugin(interceptor)
				.acceptor(acceptor)
				.transport(TcpServerTransport.create("localhost", 7000))
				.start()
				.block();

		clientRsocket = RSocketFactory.connect()
				.dataMimeType("text/plain")
				.transport(TcpClientTransport.create("localhost", 7000))
				.start()
				.block();
	}

	@AfterClass
	public static void tearDownOnce() {
		clientRsocket.dispose();
		serverChannel.dispose();
	}


	@Test
	public void fireAndForget() {

		Flux.range(1, 3)
				.concatMap(i -> clientRsocket.fireAndForget(payload("receive", "Hello " + i)))
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
				clientRsocket.requestResponse(payload("echo", "Hello " + i)).map(Payload::getDataUtf8));

		StepVerifier.create(result)
				.expectNext("Hello 1")
				.expectNext("Hello 2")
				.expectNext("Hello 3")
				.verifyComplete();
	}

	@Test
	public void echoAsync() {

		Flux<String> result = Flux.range(1, 3).concatMap(i ->
				clientRsocket.requestResponse(payload("echo-async", "Hello " + i)).map(Payload::getDataUtf8));

		StepVerifier.create(result)
				.expectNext("Hello 1 async")
				.expectNext("Hello 2 async")
				.expectNext("Hello 3 async")
				.verifyComplete();
	}

	@Test
	public void echoStream() {

		Flux<String> result = clientRsocket.requestStream(payload("echo-stream", "Hello"))
				.map(io.rsocket.Payload::getDataUtf8);

		StepVerifier.create(result)
				.expectNext("Hello 0")
				.expectNextCount(5)
				.expectNext("Hello 6")
				.expectNext("Hello 7")
				.thenCancel()
				.verify();
	}

	@Test
	public void echoChannel() {

		Flux<Payload> payloads = Flux.concat(
				Flux.just(payload("echo-channel", "Hello 1")),
				Flux.range(2, 9).map(i -> DefaultPayload.create("Hello " + i)));

		Flux<String> result = clientRsocket.requestChannel(payloads).map(Payload::getDataUtf8);

		StepVerifier.create(result)
				.expectNext("Hello 1 async")
				.expectNextCount(7)
				.expectNext("Hello 9 async")
				.expectNext("Hello 10 async")
				.verifyComplete();
	}


	private static Payload payload(String destination, String data) {
		return DefaultPayload.create(data, destination);
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

	}


	@Configuration
	static class ServerConfig {

		@Bean
		public ServerController controller() {
			return new ServerController();
		}

		@Bean
		public ReactiveSubscribableChannel rsocketChannel() {
			return new DefaultReactiveMessageChannel();
		}

		@Bean
		public RSocketMessageHandler rsocketMessageHandler() {
			RSocketMessageHandler handler = new RSocketMessageHandler(rsocketChannel());
			handler.setDecoders(Collections.singletonList(StringDecoder.allMimeTypes()));
			handler.setEncoders(Collections.singletonList(CharSequenceEncoder.allMimeTypes()));
			return handler;
		}
	}

}
