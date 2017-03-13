/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpCookie;
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
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link HttpHandlerConnector}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpHandlerConnectorTests {


	@Test
	public void adaptRequest() throws Exception {

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
		assertEquals(HttpMethod.POST, request.getMethod());
		assertEquals("/custom-path", request.getURI().toString());

		assertEquals(Arrays.asList("h0", "h1"), request.getHeaders().get("custom-header"));
		assertEquals(new HttpCookie("custom-cookie", "c0"), request.getCookies().getFirst("custom-cookie"));

		DataBuffer buffer = request.getBody().blockFirst(Duration.ZERO);
		assertEquals("Custom body", DataBufferTestUtils.dumpString(buffer, UTF_8));
	}

	@Test
	public void adaptResponse() throws Exception {

		ResponseCookie cookie = ResponseCookie.from("custom-cookie", "c0").build();

		TestHttpHandler handler = new TestHttpHandler(response -> {
			response.setStatusCode(HttpStatus.OK);
			response.getHeaders().put("custom-header", Arrays.asList("h0", "h1"));
			response.getCookies().add(cookie.getName(), cookie);
			return response.writeWith(Mono.just(toDataBuffer("Custom body")));
		});

		ClientHttpResponse response = new HttpHandlerConnector(handler)
				.connect(HttpMethod.GET, URI.create("/custom-path"), ReactiveHttpOutputMessage::setComplete)
				.block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(Arrays.asList("h0", "h1"), response.getHeaders().get("custom-header"));
		assertEquals(cookie, response.getCookies().getFirst("custom-cookie"));

		DataBuffer buffer = response.getBody().blockFirst(Duration.ZERO);
		assertEquals("Custom body", DataBufferTestUtils.dumpString(buffer, UTF_8));
	}

	private DataBuffer toDataBuffer(String body) {
		return new DefaultDataBufferFactory().wrap(body.getBytes(UTF_8));
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
