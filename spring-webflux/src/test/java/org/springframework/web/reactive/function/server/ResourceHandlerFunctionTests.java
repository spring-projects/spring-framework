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

package org.springframework.web.reactive.function.server;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResourceHandlerFunctionTests {

	private final Resource resource = new ClassPathResource("response.txt", getClass());

	private final ResourceHandlerFunction handlerFunction = new ResourceHandlerFunction(this.resource);

	private ServerResponse.Context context;


	@BeforeEach
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
			assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
			boolean condition = response instanceof EntityResponse;
			assertThat(condition).isTrue();
			@SuppressWarnings("unchecked")
					EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
			assertThat(entityResponse.entity()).isEqualTo(this.resource);
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
					assertThat(resultBytes).isEqualTo(expectedBytes);
				})
				.expectComplete()
				.verify();
		assertThat(mockResponse.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(mockResponse.getHeaders().getContentLength()).isEqualTo(this.resource.contentLength());
	}

	@Test
	public void head() throws IOException {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.head("http://localhost"));
		MockServerHttpResponse mockResponse = exchange.getResponse();

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults().messageReaders());

		Mono<ServerResponse> responseMono = this.handlerFunction.handle(request);

		Mono<Void> result = responseMono.flatMap(response -> {
			assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
			boolean condition = response instanceof EntityResponse;
			assertThat(condition).isTrue();
			@SuppressWarnings("unchecked")
			EntityResponse<Resource> entityResponse = (EntityResponse<Resource>) response;
			assertThat(entityResponse.entity().getFilename()).isEqualTo(this.resource.getFilename());
			return response.writeTo(exchange, context);
		});

		StepVerifier.create(result).expectComplete().verify();
		StepVerifier.create(mockResponse.getBody()).expectComplete().verify();

		assertThat(mockResponse.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(mockResponse.getHeaders().getContentLength()).isEqualTo(this.resource.contentLength());
	}

	@Test
	public void options() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("http://localhost"));
		MockServerHttpResponse mockResponse = exchange.getResponse();

		ServerRequest request = new DefaultServerRequest(exchange, HandlerStrategies.withDefaults().messageReaders());

		Mono<ServerResponse> responseMono = this.handlerFunction.handle(request);
		Mono<Void> result = responseMono.flatMap(response -> {
			assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.headers().getAllow()).isEqualTo(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));
			return response.writeTo(exchange, context);
		});


		StepVerifier.create(result)
				.expectComplete()
				.verify();
		assertThat(mockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(mockResponse.getHeaders().getAllow()).isEqualTo(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));

		StepVerifier.create(mockResponse.getBody()).expectComplete().verify();
	}

}
