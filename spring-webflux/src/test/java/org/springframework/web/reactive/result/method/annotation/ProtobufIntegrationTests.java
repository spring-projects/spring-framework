/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.time.Duration;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.protobuf.Msg;
import org.springframework.web.reactive.protobuf.SecondMsg;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Protobuf support.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class ProtobufIntegrationTests extends AbstractRequestMappingIntegrationTests {

	static final Msg TEST_MSG =
			Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	private WebClient webClient;


	@Override
	protected void startServer(HttpServer httpServer) throws Exception {
		super.startServer(httpServer);
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}

	@Override
	protected ApplicationContext initApplicationContext() {
		return new AnnotationConfigApplicationContext(TestConfiguration.class);
	}


	@ParameterizedHttpServerTest
	void value(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<Msg>> result = this.webClient.get()
				.uri("/message")
				.retrieve()
				.toEntity(Msg.class);

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					HttpHeaders headers = entity.getHeaders();
					assertThat(headers.getContentType().getParameters().containsKey("delimited")).isFalse();
					assertThat(headers.getFirst("X-Protobuf-Schema")).isEqualTo("sample.proto");
					assertThat(headers.getFirst("X-Protobuf-Message")).isEqualTo("Msg");
					assertThat(entity.getBody()).isEqualTo(TEST_MSG);
				})
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void values(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<List<Msg>>> result = this.webClient.get()
				.uri("/messages")
				.retrieve()
				.toEntityList(Msg.class);

		StepVerifier.create(result)
				.consumeNextWith(entity -> {
					HttpHeaders headers = entity.getHeaders();
					assertThat(headers.getContentType().getParameters().get("delimited")).isEqualTo("true");
					assertThat(headers.getFirst("X-Protobuf-Schema")).isEqualTo("sample.proto");
					assertThat(headers.getFirst("X-Protobuf-Message")).isEqualTo("Msg");
					assertThat(entity.getBody()).containsExactly(TEST_MSG, TEST_MSG, TEST_MSG);
				})
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void streaming(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Flux<Msg> result = this.webClient.get()
				.uri("/message-stream")
				.exchangeToFlux(response -> {
					assertThat(response.headers().contentType().get().getParameters().get("delimited")).isEqualTo("true");
					assertThat(response.headers().header("X-Protobuf-Schema")).containsExactly("sample.proto");
					assertThat(response.headers().header("X-Protobuf-Message")).containsExactly("Msg");
					return response.bodyToFlux(Msg.class);
				});

		StepVerifier.create(result)
				.expectNext(Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(0).build()).build())
				.expectNext(Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(1).build()).build())
				.thenCancel()
				.verify();
	}

	@ParameterizedHttpServerTest
	void empty(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<Msg> result = this.webClient.get()
				.uri("/empty")
				.retrieve()
				.bodyToMono(Msg.class);

		StepVerifier.create(result)
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void defaultInstance(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<Msg> result = this.webClient.get()
				.uri("/default-instance")
				.retrieve()
				.bodyToMono(Msg.class);

		StepVerifier.create(result)
				.verifyComplete();
	}


	@RestController
	@SuppressWarnings("unused")
	static class ProtobufController {

		@GetMapping("/message")
		Mono<Msg> message() {
			return Mono.just(TEST_MSG);
		}

		@GetMapping("/messages")
		Flux<Msg> messages() {
			return Flux.just(TEST_MSG, TEST_MSG, TEST_MSG);
		}

		@GetMapping(value = "/message-stream", produces = "application/x-protobuf;delimited=true")
		Flux<Msg> messageStream() {
			return testInterval(Duration.ofMillis(1), 5).map(l ->
					Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(l.intValue()).build()).build());
		}

		@GetMapping("/empty")
		Mono<Msg> empty() {
			return Mono.empty();
		}

		@GetMapping("default-instance")
		Mono<Msg> defaultInstance() {
			return Mono.just(Msg.getDefaultInstance());
		}
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/ProtobufIntegrationTests*.class")
	@SuppressWarnings("unused")
	static class TestConfiguration {
	}

}
