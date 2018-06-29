/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function;

import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.AbstractRouterFunctionIntegrationTests;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.junit.Assert.*;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;

public class MultipartIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final WebClient webClient = WebClient.create();

	@Test
	public void multipartData() {
		Mono<ClientResponse> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/multipartData")
				.syncBody(generateBody())
				.exchange();

		StepVerifier
				.create(result)
				.consumeNextWith(response -> assertEquals(HttpStatus.OK, response.statusCode()))
				.verifyComplete();
	}

	@Test
	public void parts() {
		Mono<ClientResponse> result = webClient
				.post()
				.uri("http://localhost:" + this.port + "/parts")
				.syncBody(generateBody())
				.exchange();

		StepVerifier
				.create(result)
				.consumeNextWith(response -> assertEquals(HttpStatus.OK, response.statusCode()))
				.verifyComplete();
	}

	private MultiValueMap<String, HttpEntity<?>> generateBody() {
		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("fooPart", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		builder.part("barPart", "bar");
		return builder.build();
	}

	@Override
	protected RouterFunction<ServerResponse> routerFunction() {
		MultipartHandler multipartHandler = new MultipartHandler();
		return route(POST("/multipartData"), multipartHandler::multipartData)
				.andRoute(POST("/parts"), multipartHandler::parts);
	}

	private static class MultipartHandler {

		public Mono<ServerResponse> multipartData(ServerRequest request) {
			return request
					.body(BodyExtractors.toMultipartData())
					.flatMap(map -> {
						Map<String, Part> parts = map.toSingleValueMap();
						try {
							assertEquals(2, parts.size());
							assertEquals("foo.txt", ((FilePart) parts.get("fooPart")).filename());
							assertEquals("bar", ((FormFieldPart) parts.get("barPart")).value());
						}
						catch(Exception e) {
							return Mono.error(e);
						}
						return ServerResponse.ok().build();
					});
		}

		public Mono<ServerResponse> parts(ServerRequest request) {
			return request.body(BodyExtractors.toParts()).collectList()
					.flatMap(parts -> {
						try {
							assertEquals(2, parts.size());
							assertEquals("foo.txt", ((FilePart) parts.get(0)).filename());
							assertEquals("bar", ((FormFieldPart) parts.get(1)).value());
						}
						catch(Exception e) {
							return Mono.error(e);
						}
						return ServerResponse.ok().build();
					});
		}
	}

}
