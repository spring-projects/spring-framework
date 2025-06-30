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

package org.springframework.web.servlet.function;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.HandlerMapping.API_VERSION_ATTRIBUTE;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
class RequestPredicatesTests {

	@Test
	void all() {
		RequestPredicate predicate = RequestPredicates.all();
		ServerRequest request = initRequest("GET", "/");
		assertThat(predicate.test(request)).isTrue();
	}


	@Test
	void method() {
		HttpMethod httpMethod = HttpMethod.GET;
		RequestPredicate predicate = RequestPredicates.method(httpMethod);

		assertThat(predicate.test(initRequest("GET", "https://example.com"))).isTrue();
		assertThat(predicate.test(initRequest("POST", "https://example.com"))).isFalse();
	}

	@Test
	void methodCorsPreFlight() {
		RequestPredicate predicate = RequestPredicates.method(HttpMethod.PUT);

		ServerRequest request = initRequest("OPTIONS", "https://example.com", servletRequest -> {
			servletRequest.addHeader("Origin", "https://example.com");
			servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");
		});
		assertThat(predicate.test(request)).isTrue();

		request = initRequest("OPTIONS", "https://example.com", servletRequest -> {
			servletRequest.removeHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
			servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		});
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void methods() {
		RequestPredicate predicate = RequestPredicates.methods(HttpMethod.GET, HttpMethod.HEAD);
		assertThat(predicate.test(initRequest("GET", "https://example.com"))).isTrue();
		assertThat(predicate.test(initRequest("HEAD", "https://example.com"))).isTrue();
		assertThat(predicate.test(initRequest("POST", "https://example.com"))).isFalse();
	}

	@Test
	void allMethods() {
		RequestPredicate predicate = RequestPredicates.GET("/p*");
		assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();

		predicate = RequestPredicates.HEAD("/p*");
		assertThat(predicate.test(initRequest("HEAD", "/path"))).isTrue();

		predicate = RequestPredicates.POST("/p*");
		assertThat(predicate.test(initRequest("POST", "/path"))).isTrue();

		predicate = RequestPredicates.PUT("/p*");
		assertThat(predicate.test(initRequest("PUT", "/path"))).isTrue();

		predicate = RequestPredicates.PATCH("/p*");
		assertThat(predicate.test(initRequest("PATCH", "/path"))).isTrue();

		predicate = RequestPredicates.DELETE("/p*");
		assertThat(predicate.test(initRequest("DELETE", "/path"))).isTrue();

		predicate = RequestPredicates.OPTIONS("/p*");
		assertThat(predicate.test(initRequest("OPTIONS", "/path"))).isTrue();
	}

	@Test
	void path() {
		RequestPredicate predicate = RequestPredicates.path("/p*");
		assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();
		assertThat(predicate.test(initRequest("GET", "/foo"))).isFalse();
	}

	@Test
	void contextPath() {
		RequestPredicate predicate = RequestPredicates.path("/bar");

		ServerRequest request = new DefaultServerRequest(
				PathPatternsTestUtils.initRequest("GET", "/foo", "/bar", true),
				Collections.emptyList());

		assertThat(predicate.test(request)).isTrue();

		request = initRequest("GET", "/foo");
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void pathNoLeadingSlash() {
		RequestPredicate predicate = RequestPredicates.path("p*");
		assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();
	}

	@Test
	void pathEncoded() {
		RequestPredicate predicate = RequestPredicates.path("/foo bar");
		assertThat(predicate.test(initRequest("GET", "/foo%20bar"))).isTrue();
		assertThat(predicate.test(initRequest("GET", ""))).isFalse();
	}

	@Test
	void pathPredicates() {
		PathPatternParser parser = new PathPatternParser();
		parser.setCaseSensitive(false);
		Function<String, RequestPredicate> pathPredicates = RequestPredicates.pathPredicates(parser);

		RequestPredicate predicate = pathPredicates.apply("/P*");
		assertThat(predicate.test(initRequest("GET", "/path"))).isTrue();
	}

	@Test
	void pathWithContext() {
		RequestPredicate predicate = RequestPredicates.path("/p*");
		ServerRequest request = initRequest("GET", "/context/path",
				servletRequest -> servletRequest.setContextPath("/context"));
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	void headers() {
		String name = "MyHeader";
		String value = "MyValue";
		RequestPredicate predicate =
				RequestPredicates.headers(
						headers -> headers.header(name).equals(Collections.singletonList(value)));
		ServerRequest request = initRequest("GET", "/path", req -> req.addHeader(name, value));
		assertThat(predicate.test(request)).isTrue();
		assertThat(predicate.test(initRequest("GET", ""))).isFalse();
	}

	@Test
	void headersCors() {
		RequestPredicate predicate = RequestPredicates.headers(headers -> false);
		ServerRequest request = initRequest("OPTIONS", "https://example.com", servletRequest -> {
			servletRequest.addHeader("Origin", "https://example.com");
			servletRequest.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT");
		});
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	void singleContentType() {
		RequestPredicate predicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON);
		ServerRequest request = initRequest("GET", "/path", r -> r.setContentType(MediaType.APPLICATION_JSON_VALUE));
		assertThat(predicate.test(request)).isTrue();

		assertThat(predicate.test(initRequest("GET", "", r -> r.setContentType(MediaType.TEXT_XML_VALUE)))).isFalse();
	}

	@Test
	void multipleContentTypes() {
		RequestPredicate predicate = RequestPredicates.contentType(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
		ServerRequest request = initRequest("GET", "/path", r -> r.setContentType(MediaType.APPLICATION_JSON_VALUE));
		assertThat(predicate.test(request)).isTrue();

		request = initRequest("GET", "/path", r -> r.setContentType(MediaType.TEXT_PLAIN_VALUE));
		assertThat(predicate.test(request)).isTrue();

		assertThat(predicate.test(initRequest("GET", "", r -> r.setContentType(MediaType.TEXT_XML_VALUE)))).isFalse();
	}

	@Test
	void singleAccept() {
		RequestPredicate predicate = RequestPredicates.accept(MediaType.APPLICATION_JSON);
		ServerRequest request = initRequest("GET", "/path", r -> r.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE));
		assertThat(predicate.test(request)).isTrue();

		request = initRequest("GET", "", req -> req.addHeader("Accept", MediaType.TEXT_XML_VALUE));
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void multipleAccepts() {
		RequestPredicate predicate = RequestPredicates.accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
		ServerRequest request = initRequest("GET", "/path", r -> r.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE));
		assertThat(predicate.test(request)).isTrue();

		request = initRequest("GET", "/path", r -> r.addHeader("Accept", MediaType.TEXT_PLAIN_VALUE));
		assertThat(predicate.test(request)).isTrue();

		request = initRequest("GET", "", req -> req.addHeader("Accept", MediaType.TEXT_XML_VALUE));
		assertThat(predicate.test(request)).isFalse();
	}

	@SuppressWarnings("removal")
	@Test
	void pathExtension() {
		RequestPredicate predicate = RequestPredicates.pathExtension("txt");

		assertThat(predicate.test(initRequest("GET", "/file.txt"))).isTrue();
		assertThat(predicate.test(initRequest("GET", "/FILE.TXT"))).isTrue();

		predicate = RequestPredicates.pathExtension("bar");
		assertThat(predicate.test(initRequest("GET", "/FILE.TXT"))).isFalse();

		assertThat(predicate.test(initRequest("GET", "/file.foo"))).isFalse();
		assertThat(predicate.test(initRequest("GET", "/file"))).isFalse();
	}

	@SuppressWarnings("removal")
	@Test
	void pathExtensionPredicate() {
		List<String> extensions = List.of("foo", "bar");
		RequestPredicate predicate = RequestPredicates.pathExtension(extensions::contains);

		assertThat(predicate.test(initRequest("GET", "/file.foo"))).isTrue();
		assertThat(predicate.test(initRequest("GET", "/file.bar"))).isTrue();
		assertThat(predicate.test(initRequest("GET", "/file"))).isFalse();
		assertThat(predicate.test(initRequest("GET", "/file.baz"))).isFalse();
	}

	@Test
	void param() {
		RequestPredicate predicate = RequestPredicates.param("foo", "bar");
		ServerRequest request = initRequest("GET", "/path", req -> req.addParameter("foo", "bar"));
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.param("foo", s -> s.equals("bar"));
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.param("foo", "baz");
		assertThat(predicate.test(request)).isFalse();

		predicate = RequestPredicates.param("foo", s -> s.equals("baz"));
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void version() {
		assertThat(RequestPredicates.version("1.1").test(serverRequest("1.1"))).isTrue();
		assertThat(RequestPredicates.version("1.1+").test(serverRequest("1.5"))).isTrue();
		assertThat(RequestPredicates.version("1.1").test(serverRequest("1.5"))).isFalse();
	}

	private static ServerRequest serverRequest(String version) {

		ApiVersionStrategy strategy = new DefaultApiVersionStrategy(
				List.of(exchange -> null), new SemanticApiVersionParser(), true, null, false, null);

		MockHttpServletRequest servletRequest =
				PathPatternsTestUtils.initRequest("GET", null, "/path", true,
						req -> req.setAttribute(API_VERSION_ATTRIBUTE, strategy.parseVersion(version)));

		return new DefaultServerRequest(servletRequest, Collections.emptyList(), strategy);
	}

	private static ServerRequest initRequest(String httpMethod, String requestUri) {
		return initRequest(httpMethod, requestUri, null);
	}

	private static ServerRequest initRequest(
			String httpMethod, String requestUri, @Nullable Consumer<MockHttpServletRequest> initializer) {

		return new DefaultServerRequest(
				PathPatternsTestUtils.initRequest(httpMethod, null, requestUri, true, initializer),
				Collections.emptyList());
	}

}
