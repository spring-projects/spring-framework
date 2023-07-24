/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.messaging.rsocket.service;

import java.time.Duration;
import java.util.stream.Stream;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Integration tests with RSocket Service client.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
class RSocketServiceIntegrationTests {

	private static CloseableChannel server;

	private static RSocketRequester requester;

	private static Service serviceProxy;

	private static ExchangeService exchangeProxy;


	@BeforeAll
	@SuppressWarnings("ConstantConditions")
	static void setupOnce() {

		MimeType metadataMimeType = MimeTypeUtils.parseMimeType(
				WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.getString());

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ServerConfig.class);
		RSocketMessageHandler messageHandler = context.getBean(RSocketMessageHandler.class);
		SocketAcceptor responder = messageHandler.responder();

		server = RSocketServer.create(responder)
				.bind(TcpServerTransport.create("localhost", 7000))
				.block();

		requester = RSocketRequester.builder()
				.metadataMimeType(metadataMimeType)
				.rsocketStrategies(context.getBean(RSocketStrategies.class))
				.tcp("localhost", 7000);

		RSocketServiceProxyFactory proxyFactory = RSocketServiceProxyFactory.builder(requester).build();
		serviceProxy = proxyFactory.createClient(Service.class);
		exchangeProxy = proxyFactory.createClient(ExchangeService.class);

		context.close();
	}

	@AfterAll
	static void tearDownOnce() {
		requester.rsocketClient().dispose();
		server.dispose();
		server = null;
		requester = null;
		serviceProxy = null;
		exchangeProxy = null;
	}


	@ParameterizedTest
	@MethodSource("getServiceProxy")
	void echoAsync(Service serviceProxy) {
		Flux<String> result = Flux.range(1, 3).concatMap(i -> serviceProxy.echoAsync("Hello " + i));

		StepVerifier.create(result)
				.expectNext("Hello 1 async").expectNext("Hello 2 async").expectNext("Hello 3 async")
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedTest
	@MethodSource("getServiceProxy")
	void echoStream(Service serviceProxy) {
		Flux<String> result = serviceProxy.echoStream("Hello");

		StepVerifier.create(result)
				.expectNext("Hello 0").expectNextCount(6).expectNext("Hello 7")
				.thenCancel()
				.verify(Duration.ofSeconds(5));
	}


	private static Stream<Arguments> getServiceProxy() {
		return Stream.of(Arguments.of(serviceProxy),
				Arguments.of(exchangeProxy));
	}


	@RSocketExchange("exchange")
	interface ExchangeService extends Service {

	}


	interface Service {

		@RSocketExchange("echo-async")
		Mono<String> echoAsync(String payload);

		@RSocketExchange("echo-stream")
		Flux<String> echoStream(String payload);

	}


	@Controller
	@RSocketExchange("exchange")
	static class ExchangeServerController implements Service {

		@Override
		public Mono<String> echoAsync(String payload) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> payload + " async");
		}

		@Override
		public Flux<String> echoStream(String payload) {
			return Flux.interval(Duration.ofMillis(10)).map(aLong -> payload + " " + aLong);
		}

	}


	@Controller
	static class ServerController {

		@MessageMapping("echo-async")
		Mono<String> echoAsync(String payload) {
			return Mono.delay(Duration.ofMillis(10)).map(aLong -> payload + " async");
		}

		@MessageMapping("echo-stream")
		Flux<String> echoStream(String payload) {
			return Flux.interval(Duration.ofMillis(10)).map(aLong -> payload + " " + aLong);
		}

	}


	@Configuration
	static class ServerConfig {

		@Bean
		ServerController controller() {
			return new ServerController();
		}

		@Bean
		ExchangeServerController exchangeController() {
			return new ExchangeServerController();
		}

		@Bean
		RSocketMessageHandler messageHandler(RSocketStrategies rsocketStrategies) {
			RSocketMessageHandler handler = new RSocketMessageHandler();
			handler.setRSocketStrategies(rsocketStrategies);
			return handler;
		}

		@Bean
		RSocketStrategies rsocketStrategies() {
			return RSocketStrategies.create();
		}

	}

}
