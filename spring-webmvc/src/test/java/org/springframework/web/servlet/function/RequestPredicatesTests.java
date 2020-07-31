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

package org.springframework.web.servlet.function;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

/**
 * @author Arjen Poutsma
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
	void contentType() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.contentType(json);
		ServerRequest request = initRequest("GET", "/path", req -> req.setContentType(json.toString()));
		assertThat(predicate.test(request)).isTrue();
		assertThat(predicate.test(initRequest("GET", ""))).isFalse();
	}

	@Test
	void accept() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.accept(json);
		ServerRequest request = initRequest("GET", "/path", req -> req.addHeader("Accept", json.toString()));
		assertThat(predicate.test(request)).isTrue();

		request = initRequest("GET", "", req -> req.addHeader("Accept", TEXT_XML_VALUE));
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	void pathExtension() {
		RequestPredicate predicate = RequestPredicates.pathExtension("txt");

		assertThat(predicate.test(initRequest("GET", "/file.txt"))).isTrue();
		assertThat(predicate.test(initRequest("GET", "/FILE.TXT"))).isTrue();

		predicate = RequestPredicates.pathExtension("bar");
		assertThat(predicate.test(initRequest("GET", "/FILE.TXT"))).isFalse();

		assertThat(predicate.test(initRequest("GET", "/file.foo"))).isFalse();
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


	private ServerRequest initRequest(String httpMethod, String requestUri) {
		return initRequest(httpMethod, requestUri, null);
	}

	private ServerRequest initRequest(
			String httpMethod, String requestUri, @Nullable Consumer<MockHttpServletRequest> initializer) {

		return new DefaultServerRequest(
				PathPatternsTestUtils.initRequest(httpMethod, null, requestUri, true, initializer),
				Collections.emptyList());
	}

}
