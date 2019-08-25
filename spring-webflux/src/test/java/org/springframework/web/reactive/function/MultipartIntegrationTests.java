/*
 * Copyright 2002-2019 the original author or authors.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.AbstractRouterFunctionIntegrationTests;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Sebastien Deleuze
 */
class MultipartIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final WebClient webClient = WebClient.create();

	private ClassPathResource resource = new ClassPathResource("org/springframework/http/codec/multipart/foo.txt");


	@ParameterizedHttpServerTest
	void multipartData(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ClientResponse> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/multipartData")
				.bodyValue(generateBody())
				.exchange();

		StepVerifier
				.create(result)
				.consumeNextWith(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void parts(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<ClientResponse> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/parts")
				.bodyValue(generateBody())
				.exchange();

		StepVerifier
				.create(result)
				.consumeNextWith(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.OK))
				.verifyComplete();
	}

	@ParameterizedHttpServerTest
	void transferTo(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Mono<String> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/transferTo")
				.bodyValue(generateBody())
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
				.verifyComplete();
	}

	private MultiValueMap<String, HttpEntity<?>> generateBody() {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("fooPart", resource);
		builder.part("barPart", "bar");
		return builder.build();
	}

	@Override
	protected RouterFunction<ServerResponse> routerFunction() {
		MultipartHandler multipartHandler = new MultipartHandler();
		return route()
				.POST("/multipartData", multipartHandler::multipartData)
				.POST("/parts", multipartHandler::parts)
				.POST("/transferTo", multipartHandler::transferTo)
				.build();
	}


	private static class MultipartHandler {

		public Mono<ServerResponse> multipartData(ServerRequest request) {
			return request
					.body(BodyExtractors.toMultipartData())
					.flatMap(map -> {
						Map<String, Part> parts = map.toSingleValueMap();
						try {
							assertThat(parts.size()).isEqualTo(2);
							assertThat(((FilePart) parts.get("fooPart")).filename()).isEqualTo("foo.txt");
							assertThat(((FormFieldPart) parts.get("barPart")).value()).isEqualTo("bar");
							return ServerResponse.ok().build();
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
							assertThat(parts.size()).isEqualTo(2);
							assertThat(((FilePart) parts.get(0)).filename()).isEqualTo("foo.txt");
							assertThat(((FormFieldPart) parts.get(1)).value()).isEqualTo("bar");
							return ServerResponse.ok().build();
						}
						catch(Exception e) {
							return Mono.error(e);
						}
					});
		}

		public Mono<ServerResponse> transferTo(ServerRequest request) {
			return request.body(BodyExtractors.toParts())
					.filter(part -> part instanceof FilePart)
					.next()
					.cast(FilePart.class)
					.flatMap(part -> {
						try {
							Path tempFile = Files.createTempFile("MultipartIntegrationTests", null);
							return part.transferTo(tempFile)
									.then(ServerResponse.ok()
											.bodyValue(tempFile.toString()));
						}
						catch (Exception e) {
							return Mono.error(e);
						}
					});
		}

	}

}
