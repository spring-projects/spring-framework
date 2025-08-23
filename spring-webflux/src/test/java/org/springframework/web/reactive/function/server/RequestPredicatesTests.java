/*
 * Copyright 2002-present the original author or authors.
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
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.ApiVersionStrategy;
import org.springframework.web.reactive.accept.DefaultApiVersionStrategy;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
class RequestPredicatesTests {


	@Test
	void all() {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com").build();
		MockServerWebExchange mockExchange = MockServerWebExchange.from(mockRequest);
		RequestPredicate predicate = RequestPredicates.all();
		ServerRequest request = new DefaultServerRequest(mockExchange, Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	void method() {
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
	void methodCorsPreFlight() {
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
	void methods() {
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
	void allMethods() {
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
	void path() {
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
	void pathNoLeadingSlash() {
		RequestPredicate predicate = RequestPredicates.path("p*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com/path").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	void pathEncoded() {
		URI uri = URI.create("https://localhost/foo%20bar");
		RequestPredicate predicate = RequestPredicates.path("/foo bar");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	void pathPredicates() {
		PathPatternParser parser = new PathPatternParser();
		parser.setCaseSensitive(false);
		Function<String, RequestPredicate> pathPredicates = RequestPredicates.pathPredicates(parser);

		RequestPredicate predicate = pathPredicates.apply("/P*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com/path").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	void pathWithContext() {
		RequestPredicate predicate = RequestPredicates.path("/p*");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://localhost/context/path")
				.contextPath("/context").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	void headers() {
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
	void headersCors() {
		RequestPredicate predicate = RequestPredicates.headers(headers -> false);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.options("https://example.com")
				.header("Origin", "https://example.com")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();
	}


	@Test
	void singleContentType() {
		RequestPredicate predicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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
	void multipleContentTypes() {
		RequestPredicate predicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.CONTENT_TYPE, "foo/bar")
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void singleAccept() {
		RequestPredicate predicate = RequestPredicates.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
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
	void multipleAccepts() {
		RequestPredicate predicate = RequestPredicates.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE)
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		mockRequest = MockServerHttpRequest.get("https://example.com")
				.header(HttpHeaders.ACCEPT, "foo/bar")
				.build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@SuppressWarnings("removal")
	@Test
	void pathExtension() {
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

	@SuppressWarnings("removal")
	@Test
	void pathExtensionPredicate() {
		List<String> extensions = List.of("foo", "bar");
		RequestPredicate predicate = RequestPredicates.pathExtension(extensions::contains);

		URI uri = URI.create("https://localhost/file.foo");
		MockServerHttpRequest mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		uri = URI.create("https://localhost/file.bar");
		mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isTrue();

		uri = URI.create("https://localhost/file");
		mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();

		uri = URI.create("https://localhost/file.baz");
		mockRequest = MockServerHttpRequest.method(HttpMethod.GET, uri).build();
		request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void queryParam() {
		MockServerHttpRequest mockRequest = MockServerHttpRequest.get("https://example.com")
				.queryParam("foo", "bar").build();
		ServerRequest request = new DefaultServerRequest(MockServerWebExchange.from(mockRequest), Collections.emptyList());
		RequestPredicate predicate = RequestPredicates.queryParam("foo", s -> s.equals("bar"));
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.queryParam("foo", s -> s.equals("baz"));
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void version() {
		assertThat(RequestPredicates.version("1.1").test(serverRequest("1.1"))).isTrue();
		assertThat(RequestPredicates.version("1.1+").test(serverRequest("1.5"))).isTrue();
		assertThat(RequestPredicates.version("1.1").test(serverRequest("1.5"))).isFalse();
	}

	private static DefaultServerRequest serverRequest(String version) {
		ApiVersionStrategy versionStrategy = apiVersionStrategy();
		Comparable<?> parsedVersion = versionStrategy.parseVersion(version);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://localhost"));
		exchange.getAttributes().put(HandlerMapping.API_VERSION_ATTRIBUTE, parsedVersion);
		return new DefaultServerRequest(exchange, Collections.emptyList(), versionStrategy);
	}

	private static DefaultApiVersionStrategy apiVersionStrategy() {
		return new DefaultApiVersionStrategy(
				List.of(exchange -> null), new SemanticApiVersionParser(), true, null, false, null, null);
	}

}
