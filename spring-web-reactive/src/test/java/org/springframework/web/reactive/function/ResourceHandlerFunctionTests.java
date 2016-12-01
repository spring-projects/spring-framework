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

package org.springframework.web.reactive.function;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.junit.Before;
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
import static org.junit.Assert.assertNull;

/**
 * @author Arjen Poutsma
 */
public class ResourceHandlerFunctionTests {

	private Resource resource;

	private ResourceHandlerFunction handlerFunction;

	@Before
	public void createResource() {
		this.resource = new ClassPathResource("response.txt", getClass());
		this.handlerFunction = new ResourceHandlerFunction(this.resource);
	}

	@Test
	public void get() throws IOException {
		MockServerHttpRequest mockRequest =
				new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse mockResponse = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(mockRequest, mockResponse,
				new MockWebSessionManager());

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults());

		ServerResponse<Resource> response = this.handlerFunction.handle(request);
		assertEquals(HttpStatus.OK, response.statusCode());
		assertEquals(this.resource, response.body());

		Mono<Void> result = response.writeTo(exchange, HandlerStrategies.withDefaults());

		StepVerifier.create(result)
				.expectComplete()
				.verify();

		StepVerifier.create(result).expectComplete().verify();

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
		assertEquals(49, mockResponse.getHeaders().getContentLength());
	}

	@Test
	public void head() throws IOException {
		MockServerHttpRequest mockRequest =
				new MockServerHttpRequest(HttpMethod.HEAD, "http://localhost");
		MockServerHttpResponse mockResponse = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(mockRequest, mockResponse,
				new MockWebSessionManager());

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults());

		ServerResponse<Resource> response = this.handlerFunction.handle(request);
		assertEquals(HttpStatus.OK, response.statusCode());

		Mono<Void> result = response.writeTo(exchange, HandlerStrategies.withDefaults());

		StepVerifier.create(result)
				.expectComplete()
				.verify();

		StepVerifier.create(result).expectComplete().verify();

		StepVerifier.create(mockResponse.getBody())
				.expectComplete()
				.verify();
		assertEquals(MediaType.TEXT_PLAIN, mockResponse.getHeaders().getContentType());
		assertEquals(49, mockResponse.getHeaders().getContentLength());
	}

	@Test
	public void options() {
		MockServerHttpRequest mockRequest =
				new MockServerHttpRequest(HttpMethod.OPTIONS, "http://localhost");
		MockServerHttpResponse mockResponse = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(mockRequest, mockResponse,
				new MockWebSessionManager());

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults());

		ServerResponse<Resource> response = this.handlerFunction.handle(request);

		assertEquals(HttpStatus.OK, response.statusCode());
		assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS),
				response.headers().getAllow());
		assertNull(response.body());

		Mono<Void> result = response.writeTo(exchange, HandlerStrategies.withDefaults());

		StepVerifier.create(result)
				.expectComplete()
				.verify();
		assertEquals(HttpStatus.OK, mockResponse.getStatusCode());
		assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS),
				mockResponse.getHeaders().getAllow());

		assertNull(mockResponse.getBody());
	}

}