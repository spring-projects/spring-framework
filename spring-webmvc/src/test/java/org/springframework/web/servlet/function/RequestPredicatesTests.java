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

package org.springframework.web.servlet.function;

import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.util.pattern.PathPatternParser;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

/**
 * @author Arjen Poutsma
 */
public class RequestPredicatesTests {

	@Test
	public void all() {
		RequestPredicate predicate = RequestPredicates.all();
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();
	}


	@Test
	public void method() {
		HttpMethod httpMethod = HttpMethod.GET;
		RequestPredicate predicate = RequestPredicates.method(httpMethod);

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "https://example.com");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();

		servletRequest.setMethod("POST");
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void methods() {
		RequestPredicate predicate = RequestPredicates.methods(HttpMethod.GET, HttpMethod.HEAD);
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "https://example.com");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();

		servletRequest.setMethod("HEAD");
		assertThat(predicate.test(request)).isTrue();

		servletRequest.setMethod("POST");
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void allMethods() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());

		RequestPredicate predicate = RequestPredicates.GET("/p*");
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.HEAD("/p*");
		servletRequest.setMethod("HEAD");
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.POST("/p*");
		servletRequest.setMethod("POST");
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.PUT("/p*");
		servletRequest.setMethod("PUT");
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.PATCH("/p*");
		servletRequest.setMethod("PATCH");
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.DELETE("/p*");
		servletRequest.setMethod("DELETE");
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.OPTIONS("/p*");
		servletRequest.setMethod("OPTIONS");
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void path() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		RequestPredicate predicate = RequestPredicates.path("/p*");
		assertThat(predicate.test(request)).isTrue();

		servletRequest = new MockHttpServletRequest("GET", "/foo");
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void servletPath() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/foo/bar");
		servletRequest.setServletPath("/foo");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		RequestPredicate predicate = RequestPredicates.path("/bar");
		assertThat(predicate.test(request)).isTrue();

		servletRequest = new MockHttpServletRequest("GET", "/foo");
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathNoLeadingSlash() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		RequestPredicate predicate = RequestPredicates.path("p*");
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void pathEncoded() {
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/foo%20bar");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());

		RequestPredicate predicate = RequestPredicates.path("/foo bar");
		assertThat(predicate.test(request)).isTrue();

		servletRequest = new MockHttpServletRequest();
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathPredicates() {
		PathPatternParser parser = new PathPatternParser();
		parser.setCaseSensitive(false);
		Function<String, RequestPredicate> pathPredicates = RequestPredicates.pathPredicates(parser);

		RequestPredicate predicate = pathPredicates.apply("/P*");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();
	}


	@Test
	public void headers() {
		String name = "MyHeader";
		String value = "MyValue";
		RequestPredicate predicate =
				RequestPredicates.headers(
						headers -> headers.header(name).equals(Collections.singletonList(value)));
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		servletRequest.addHeader(name, value);
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();

		servletRequest = new MockHttpServletRequest();
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void contentType() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.contentType(json);
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		servletRequest.setContentType(json.toString());
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());

		assertThat(predicate.test(request)).isTrue();

		servletRequest = new MockHttpServletRequest();
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void accept() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.accept(json);
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		servletRequest.addHeader("Accept", json.toString());
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();

		servletRequest = new MockHttpServletRequest();
		servletRequest.addHeader("Accept", TEXT_XML_VALUE);
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathExtension() {
		RequestPredicate predicate = RequestPredicates.pathExtension("txt");

		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/file.txt");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();

		servletRequest = new MockHttpServletRequest("GET", "/FILE.TXT");
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.pathExtension("bar");
		assertThat(predicate.test(request)).isFalse();

		servletRequest = new MockHttpServletRequest("GET", "/file.foo");
		request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void param() {
		RequestPredicate predicate = RequestPredicates.param("foo", "bar");
		MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/path");
		servletRequest.addParameter("foo", "bar");
		ServerRequest request = new DefaultServerRequest(servletRequest, emptyList());
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.param("foo", s -> s.equals("bar"));
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.param("foo", "baz");
		assertThat(predicate.test(request)).isFalse();

		predicate = RequestPredicates.param("foo", s -> s.equals("baz"));
		assertThat(predicate.test(request)).isFalse();
	}

}
