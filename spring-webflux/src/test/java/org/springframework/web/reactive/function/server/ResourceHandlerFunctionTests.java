/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.reactive.result.view.ViewResolver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResourceHandlerFunctionTests {

	private final Resource resource = new ClassPathResource("response.txt", getClass());

	private final ResourceHandlerFunction handlerFunction = new ResourceHandlerFunction(this.resource);

	private ServerResponse.Context context;

	@Before
	public void createContext() {
		HandlerStrategies strategies = HandlerStrategies.withDefaults();
		context = new ServerResponse.Context() {
			@Override
			public List<HttpMessageWriter<?>> messageWriters() {
				return strategies.messageWriters();
			}

			@Override
			public List<ViewResolver> viewResolvers() {
				return strategies.viewResolvers();
			}
		};

	}


	@Test
	public void get() throws IOException {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost"));
		MockServerHttpResponse mockResponse = exchange.getResponse();

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults().messageReaders());

		Mono<ServerResponse> responseMono = this.handlerFunction.handle(request);

		Mono<Void> result = responseMono.flatMap(response -> {
					assertEquals(HttpStatus.OK, response.statusCode());
					assertTrue(response instanceof EntityResponse);
					@SuppressWarnings("unchecked")
					EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
					assertEquals(this.resource, entityResponse.entity());
					return response.writeTo(exchange, context);
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
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.head("http://localhost"));
		MockServerHttpResponse mockResponse = exchange.getResponse();

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults().messageReaders());

		Mono<ServerResponse> responseMono = this.handlerFunction.handle(request);

		Mono<Void> result = responseMono.flatMap(response -> {
			assertEquals(HttpStatus.OK, response.statusCode());
			assertTrue(response instanceof EntityResponse);
			@SuppressWarnings("unchecked")
			EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
			assertEquals(this.resource.getFilename(), entityResponse.entity().getFilename());
			return response.writeTo(exchange, context);
		});

		StepVerifier.create(result).expectComplete().verify();
		StepVerifier.create(mockResponse.getBody()).expectComplete().verify();

		assertEquals(MediaType.TEXT_PLAIN, mockResponse.getHeaders().getContentType());
		assertEquals(this.resource.contentLength(), mockResponse.getHeaders().getContentLength());
	}

	@Test
	public void options() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("http://localhost"));
		MockServerHttpResponse mockResponse = exchange.getResponse();

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults().messageReaders());

		Mono<ServerResponse> responseMono = this.handlerFunction.handle(request);
		Mono<Void> result = responseMono.flatMap(response -> {
			assertEquals(HttpStatus.OK, response.statusCode());
			assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS),
					response.headers().getAllow());
			return response.writeTo(exchange, context);
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
