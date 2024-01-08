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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpHandlerConnector}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpHandlerConnectorTests {

	@Test
	public void adaptRequest() {

		TestHttpHandler handler = new TestHttpHandler(response -> {
			response.setStatusCode(HttpStatus.OK);
			return response.setComplete();
		});

		new HttpHandlerConnector(handler).connect(HttpMethod.POST, URI.create("/custom-path"),
				request -> {
					request.getHeaders().put("custom-header", Arrays.asList("h0", "h1"));
					request.getCookies().add("custom-cookie", new HttpCookie("custom-cookie", "c0"));
					return request.writeWith(Mono.just(toDataBuffer("Custom body")));
				}).block(Duration.ofSeconds(5));

		MockServerHttpRequest request = (MockServerHttpRequest) handler.getSavedRequest();
		assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
		assertThat(request.getURI().toString()).isEqualTo("/custom-path");

		HttpHeaders headers = request.getHeaders();
		assertThat(headers.get("custom-header")).isEqualTo(Arrays.asList("h0", "h1"));
		assertThat(request.getCookies().getFirst("custom-cookie")).isEqualTo(new HttpCookie("custom-cookie", "c0"));
		assertThat(headers.get(HttpHeaders.COOKIE)).isEqualTo(Collections.singletonList("custom-cookie=c0"));

		DataBuffer buffer = request.getBody().blockFirst(Duration.ZERO);
		assertThat(buffer.toString(UTF_8)).isEqualTo("Custom body");
	}

	@Test
	public void adaptResponse() {

		ResponseCookie cookie = ResponseCookie.from("custom-cookie", "c0").build();

		TestHttpHandler handler = new TestHttpHandler(response -> {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().put("custom-header", Arrays.asList("h0", "h1"));
			response.addCookie(cookie);
			return response.writeWith(Mono.just(toDataBuffer("Custom body")));
		});

		ClientHttpResponse response = new HttpHandlerConnector(handler)
				.connect(HttpMethod.GET, URI.create("/custom-path"), ReactiveHttpOutputMessage::setComplete)
				.block(Duration.ofSeconds(5));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		HttpHeaders headers = response.getHeaders();
		assertThat(headers.get("custom-header")).isEqualTo(Arrays.asList("h0", "h1"));
		assertThat(response.getCookies().getFirst("custom-cookie")).isEqualTo(cookie);
		assertThat(headers.get(HttpHeaders.SET_COOKIE)).isEqualTo(Collections.singletonList("custom-cookie=c0"));

		DataBuffer buffer = response.getBody().blockFirst(Duration.ZERO);
		assertThat(buffer.toString(UTF_8)).isEqualTo("Custom body");
	}

	@Test // gh-23936
	public void handlerOnNonBlockingThread() {

		TestHttpHandler handler = new TestHttpHandler(response -> {

			assertThat(Schedulers.isInNonBlockingThread()).isTrue();

			response.setStatusCode(HttpStatus.OK);
			return response.setComplete();
		});

		new HttpHandlerConnector(handler)
				.connect(HttpMethod.POST, URI.create("/path"), request -> request.writeWith(Mono.empty()))
				.block(Duration.ofSeconds(5));
	}

	private DataBuffer toDataBuffer(String body) {
		return DefaultDataBufferFactory.sharedInstance.wrap(body.getBytes(UTF_8));
	}


	private static class TestHttpHandler implements HttpHandler {

		private ServerHttpRequest savedRequest;

		private final Function<ServerHttpResponse, Mono<Void>> responseMonoFunction;


		public TestHttpHandler(Function<ServerHttpResponse, Mono<Void>> function) {
			this.responseMonoFunction = function;
		}

		public ServerHttpRequest getSavedRequest() {
			return this.savedRequest;
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			this.savedRequest = request;
			return this.responseMonoFunction.apply(response);
		}
	}

}
