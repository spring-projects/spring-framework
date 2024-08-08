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

package org.springframework.web.reactive.function;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.AbstractRouterFunctionIntegrationTests;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.UndertowHttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Sebastien Deleuze
 */
class MultipartRouterFunctionIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final WebClient webClient = WebClient.create();

	private final ClassPathResource resource = new ClassPathResource("foo.txt", getClass());


	@ParameterizedHttpServerTest
	void multipartData(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<Void>> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/multipartData")
				.body(generateBody(), PartEvent.class)
				.retrieve()
				.toEntity(Void.class);

		StepVerifier
				.create(result)
				.consumeNextWith(entity -> assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedHttpServerTest
	void parts(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<Void>> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/parts")
				.body(generateBody(), PartEvent.class)
				.retrieve()
				.toEntity(Void.class);

		StepVerifier
				.create(result)
				.consumeNextWith(entity -> assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedHttpServerTest
	void transferTo(HttpServer httpServer) throws Exception {
		// TODO Determine why Undertow fails: https://github.com/spring-projects/spring-framework/issues/25310
		assumeFalse(httpServer instanceof UndertowHttpServer, "Undertow currently fails with transferTo");
		verifyTransferTo(httpServer);
	}

	@Disabled("Unstable on Undertow: https://github.com/spring-projects/spring-framework/issues/25310")
	// Using @RepeatedTest(100), this test fails approximately 10% - 20% of the time.
	@Test
	void transferToWithUndertow() throws Exception {
		verifyTransferTo(new UndertowHttpServer());
	}

	private void verifyTransferTo(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/transferTo")
				.body(generateBody(), PartEvent.class)
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier
				.create(result)
				.consumeNextWith(location -> {
					try {
						byte[] actualBytes = Files.readAllBytes(Paths.get(location));
						byte[] expectedBytes = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
						assertThat(actualBytes).isEqualTo(expectedBytes);
					}
					catch (IOException ex) {
						fail("IOException", ex);
					}
				})
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedHttpServerTest
	void partData(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ResponseEntity<Void>> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/partData")
				.body(generateBody(), PartEvent.class)
				.retrieve()
				.toEntity(Void.class);

		StepVerifier
				.create(result)
				.consumeNextWith(entity -> assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@ParameterizedHttpServerTest
	void proxy(HttpServer httpServer) throws Exception {
		assumeFalse(httpServer instanceof UndertowHttpServer, "Undertow currently fails proxying requests");
		startServer(httpServer);

		Mono<ResponseEntity<Void>> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/proxy")
				.body(generateBody(), PartEvent.class)
				.retrieve()
				.toEntity(Void.class);

		StepVerifier
				.create(result)
				.consumeNextWith(entity -> assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}


	private Flux<PartEvent> generateBody() {
		return Flux.concat(
				FilePartEvent.create("fooPart", this.resource),
				FormPartEvent.create("barPart", "bar")
		);
	}

	@Override
	protected RouterFunction<ServerResponse> routerFunction() {
		MultipartHandler multipartHandler = new MultipartHandler();
		return route()
				.POST("/multipartData", multipartHandler::multipartData)
				.POST("/parts", multipartHandler::parts)
				.POST("/transferTo", multipartHandler::transferTo)
				.POST("/partData", multipartHandler::partData)
				.POST("/proxy", multipartHandler::proxy)
				.build();
	}


	private static class MultipartHandler {

		public Mono<ServerResponse> multipartData(ServerRequest request) {
			return request
					.body(BodyExtractors.toMultipartData())
					.flatMap(map -> {
						Map<String, Part> parts = map.asSingleValueMap();
						try {
							assertThat(parts).hasSize(2);
							assertThat(((FilePart) parts.get("fooPart")).filename()).isEqualTo("foo.txt");
							assertThat(((FormFieldPart) parts.get("barPart")).value()).isEqualTo("bar");
							return Flux.fromIterable(parts.values())
									.concatMap(Part::delete)
									.then(ServerResponse.ok().build());
						}
						catch(Exception e) {
							return Mono.error(e);
						}
					});
		}

		public Mono<ServerResponse> parts(ServerRequest request) {
			return request.body(BodyExtractors.toParts()).collectList()
					.flatMap(parts -> {
						try {
							assertThat(parts).hasSize(2);
							assertThat(((FilePart) parts.get(0)).filename()).isEqualTo("foo.txt");
							assertThat(((FormFieldPart) parts.get(1)).value()).isEqualTo("bar");
							return Flux.fromIterable(parts)
									.concatMap(Part::delete)
									.then(ServerResponse.ok().build());
						}
						catch(Exception e) {
							return Mono.error(e);
						}
					});
		}

		public Mono<ServerResponse> transferTo(ServerRequest request) {
			return request.body(BodyExtractors.toParts())
					.ofType(FilePart.class)
					.next()
					.flatMap(part -> createTempFile()
							.flatMap(tempFile ->
									part.transferTo(tempFile)
											.then(ServerResponse.ok().bodyValue(tempFile.toString()))));
		}

		public Mono<ServerResponse> partData(ServerRequest request) {
			return request.bodyToFlux(PartEvent.class)
					.bufferUntil(PartEvent::isLast)
					.collectList()
					.flatMap((List<List<PartEvent>> data) -> {
						assertThat(data).hasSize(2);
						assertThat(data.get(0)).satisfiesExactly(
								zero -> assertThat(zero).isInstanceOfSatisfying(FilePartEvent.class, filePartEvent -> {
									assertThat(filePartEvent.name()).isEqualTo("fooPart");
									assertThat(filePartEvent.filename()).isEqualTo("foo.txt");
									DataBufferUtils.release(filePartEvent.content());
								}),
								one -> assertThat(one).isInstanceOfSatisfying(FilePartEvent.class, filePartEvent -> {
									assertThat(filePartEvent.name()).isEqualTo("fooPart");
									assertThat(filePartEvent.filename()).isEqualTo("foo.txt");
									DataBufferUtils.release(filePartEvent.content());
								}));
						assertThat(data.get(1)).singleElement().isInstanceOfSatisfying(FormPartEvent.class, formPartEvent -> {
							assertThat(formPartEvent.name()).isEqualTo("barPart");
							assertThat(formPartEvent.content().toString(StandardCharsets.UTF_8)).isEqualTo("bar");
							DataBufferUtils.release(formPartEvent.content());
						});
						return ServerResponse.ok().build();
					});
		}

		public Mono<ServerResponse> proxy(ServerRequest request) {
			return Mono.defer(() -> {
				WebClient client = WebClient.create("http://localhost:" + request.uri().getPort() + "/multipartData");
				return client.post()
						.body(request.bodyToFlux(PartEvent.class), PartEvent.class)
						.retrieve()
						.toEntity(Void.class)
						.flatMap(response -> ServerResponse.ok().build());
			});
		}

		private Mono<Path> createTempFile() {
			return Mono.defer(() -> {
				try {
					return Mono.just(Files.createTempFile("MultipartIntegrationTests", null));
				}
				catch (IOException ex) {
					return Mono.error(ex);
				}
			}).subscribeOn(Schedulers.boundedElastic());
		}

	}

}
