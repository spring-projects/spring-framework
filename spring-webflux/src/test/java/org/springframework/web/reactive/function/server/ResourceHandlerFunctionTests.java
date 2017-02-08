/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Arjen Poutsma
 */
public class ResourceHandlerFunctionTests {

	private final Resource resource = new ClassPathResource("response.txt", getClass());

	private final ResourceHandlerFunction handlerFunction = new ResourceHandlerFunction(this.resource);


	@Test
	public void get() throws IOException {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("http://localhost").build();
		MockServerHttpResponse mockResponse = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(mockRequest, mockResponse,
				new MockWebSessionManager());

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults());

		Mono<ServerResponse> responseMono = this.handlerFunction.handle(request);

		Mono<Void> result = responseMono.then(response -> {
					assertEquals(HttpStatus.OK, response.statusCode());
/*
TODO: enable when ServerEntityResponse is reintroduced
					StepVerifier.create(response.body())
							.expectNext(this.resource)
							.expectComplete()
							.verify();
*/
					return response.writeTo(exchange, HandlerStrategies.withDefaults());
				});

		StepVerifier.create(result)
				.expectComplete()
				.verify();

		byte[] expectedBytes = Files.readAllBytes(this.resource.getFile().toPath());

		StepVerifier.create(mockResponse.getBody())
				.consumeNextWith(dataBuffer -> {
					byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(resultBytes);
					assertArrayEquals(expectedBytes, resultBytes);
				})
				.expectComplete()
				.verify();
		assertEquals(MediaType.TEXT_PLAIN, mockResponse.getHeaders().getContentType());
		assertEquals(this.resource.contentLength(), mockResponse.getHeaders().getContentLength());
	}

	@Test
	public void head() throws IOException {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.head("http://localhost").build();
		MockServerHttpResponse mockResponse = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(mockRequest, mockResponse,
				new MockWebSessionManager());

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults());

		Mono<ServerResponse> response = this.handlerFunction.handle(request);

		Mono<Void> result = response.then(res -> {
			assertEquals(HttpStatus.OK, res.statusCode());
			return res.writeTo(exchange, HandlerStrategies.withDefaults());
		});

		StepVerifier.create(result)
				.expectComplete()
				.verify();

		StepVerifier.create(result).expectComplete().verify();

		StepVerifier.create(mockResponse.getBody())
				.expectComplete()
				.verify();
		assertEquals(MediaType.TEXT_PLAIN, mockResponse.getHeaders().getContentType());
		assertEquals(this.resource.contentLength(), mockResponse.getHeaders().getContentLength());
	}

	@Test
	public void options() {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.options("http://localhost").build();
		MockServerHttpResponse mockResponse = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(mockRequest, mockResponse,
				new MockWebSessionManager());

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults());

		Mono<ServerResponse> responseMono = this.handlerFunction.handle(request);
		Mono<Void> result = responseMono.then(response -> {
			assertEquals(HttpStatus.OK, response.statusCode());
			assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS),
					response.headers().getAllow());
/*
TODO: enable when ServerEntityResponse is reintroduced
			StepVerifier.create(response.body())
					.expectComplete()
					.verify();
*/
			return response.writeTo(exchange, HandlerStrategies.withDefaults());
		});


		StepVerifier.create(result)
				.expectComplete()
				.verify();
		assertEquals(HttpStatus.OK, mockResponse.getStatusCode());
		assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS),
				mockResponse.getHeaders().getAllow());

		StepVerifier.create(mockResponse.getBody()).expectComplete().verify();
	}

}
