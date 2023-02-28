/*
 * Copyright 2002-2020 the original author or authors.
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

import java.net.URI;
import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RequestPredicatesTests {


	@Test
	public void all() {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);
		RequestPredicate predicate = RequestPredicates.all();
		ServerRequest request = new DefaultServerRequest(mockExchange, Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void method() {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();

		HttpMethod httpMethod = HttpMethod.GET;
		RequestPredicate predicate = RequestPredicates.method(httpMethod);
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.post("https://example.com").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void methodCorsPreFlight() {
		RequestPredicate predicate = RequestPredicates.method(HttpMethod.PUT);

		MockServerHttpRequest mockRequest = MockServerHttpRequest.options("https://example.com")
				.header("Origin", "https://example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());

		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.options("https://example.com")
				.header("Origin", "https://example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}


	@Test
	public void methods() {
		RequestPredicate predicate = RequestPredicates.methods(HttpMethod.GET, HttpMethod.HEAD);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.head("https://example.com").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.post("https://example.com").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void allMethods() {
		RequestPredicate predicate = RequestPredicates.GET("/p*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com/path").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.HEAD("/p*");
		mockRequest = MockServerHttpRequest.head("https://example.com/path").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.POST("/p*");
		mockRequest = MockServerHttpRequest.post("https://example.com/path").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.PUT("/p*");
		mockRequest = MockServerHttpRequest.put("https://example.com/path").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.PATCH("/p*");
		mockRequest = MockServerHttpRequest.patch("https://example.com/path").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.DELETE("/p*");
		mockRequest = MockServerHttpRequest.delete("https://example.com/path").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.OPTIONS("/p*");
		mockRequest = MockServerHttpRequest.options("https://example.com/path").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void path() {
		URI uri = URI.create("https://localhost/path");
		RequestPredicate predicate = RequestPredicates.path("/p*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get(uri.toString()).build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.head("https://example.com").build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathNoLeadingSlash() {
		RequestPredicate predicate = RequestPredicates.path("p*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com/path").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void pathEncoded() {
		URI uri = URI.create("https://localhost/foo%20bar");
		RequestPredicate predicate = RequestPredicates.path("/foo bar");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void pathPredicates() {
		PathPatternParser parser = new PathPatternParser();
		parser.setCaseSensitive(false);
		Function<String, RequestPredicate> pathPredicates = RequestPredicates.pathPredicates(parser);

		RequestPredicate predicate = pathPredicates.apply("/P*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com/path").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void pathWithContext() {
		RequestPredicate predicate = RequestPredicates.path("/p*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/context/path")
				.contextPath("/context").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void headers() {
		String name = "MyHeader";
		String value = "MyValue";
		RequestPredicate predicate =
				RequestPredicates.headers(
						headers -> headers.header(name).equals(Collections.singletonList(value)));
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(name, value)
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(name, "bar")
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void headersCors() {
		RequestPredicate predicate = RequestPredicates.headers(headers -> false);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.options("https://example.com")
				.header("Origin", "https://example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}


	@Test
	public void contentType() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.contentType(json);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.CONTENT_TYPE, json.toString())
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.CONTENT_TYPE, "foo/bar")
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void accept() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.accept(json);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.ACCEPT, json.toString())
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.ACCEPT, "foo/bar")
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathExtension() {
		RequestPredicate predicate = RequestPredicates.pathExtension("txt");

		URI uri = URI.create("https://localhost/file.txt");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		uri = URI.create("https://localhost/FILE.TXT");
		mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.pathExtension("bar");
		assertThat(predicate.test(request)).isFalse();

		uri = URI.create("https://localhost/file.foo");
		mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void queryParam() {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.queryParam("foo", "bar").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		RequestPredicate predicate = RequestPredicates.queryParam("foo", s -> s.equals("bar"));
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.queryParam("foo", s -> s.equals("baz"));
		assertThat(predicate.test(request)).isFalse();
	}

}
