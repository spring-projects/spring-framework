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

import java.net.URI;
import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RequestPredicatesTests {

	@Test
	public void all() {
		RequestPredicate predicate = RequestPredicates.all();
		MockServerRequest request = MockServerRequest.builder().build();
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void method() {
		HttpMethod httpMethod = HttpMethod.GET;
		RequestPredicate predicate = RequestPredicates.method(httpMethod);
		MockServerRequest request = MockServerRequest.builder().method(httpMethod).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().method(HttpMethod.POST).build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void methods() {
		RequestPredicate predicate = RequestPredicates.methods(HttpMethod.GET, HttpMethod.HEAD);
		MockServerRequest request = MockServerRequest.builder().method(HttpMethod.GET).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().method(HttpMethod.HEAD).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().method(HttpMethod.POST).build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void allMethods() {
		URI uri = URI.create("http://localhost/path");

		RequestPredicate predicate = RequestPredicates.GET("/p*");
		MockServerRequest request = MockServerRequest.builder().method(HttpMethod.GET).uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.HEAD("/p*");
		request = MockServerRequest.builder().method(HttpMethod.HEAD).uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.POST("/p*");
		request = MockServerRequest.builder().method(HttpMethod.POST).uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.PUT("/p*");
		request = MockServerRequest.builder().method(HttpMethod.PUT).uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.PATCH("/p*");
		request = MockServerRequest.builder().method(HttpMethod.PATCH).uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.DELETE("/p*");
		request = MockServerRequest.builder().method(HttpMethod.DELETE).uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.OPTIONS("/p*");
		request = MockServerRequest.builder().method(HttpMethod.OPTIONS).uri(uri).build();
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void path() {
		URI uri = URI.create("http://localhost/path");
		RequestPredicate predicate = RequestPredicates.path("/p*");
		MockServerRequest request = MockServerRequest.builder().uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathNoLeadingSlash() {
		URI uri = URI.create("http://localhost/path");
		RequestPredicate predicate = RequestPredicates.path("p*");
		MockServerRequest request = MockServerRequest.builder().uri(uri).build();
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void pathEncoded() {
		URI uri = URI.create("http://localhost/foo%20bar");
		RequestPredicate predicate = RequestPredicates.path("/foo bar");
		MockServerRequest request = MockServerRequest.builder().uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathPredicates() {
		PathPatternParser parser = new PathPatternParser();
		parser.setCaseSensitive(false);
		Function<String, RequestPredicate> pathPredicates = RequestPredicates.pathPredicates(parser);

		URI uri = URI.create("http://localhost/path");
		RequestPredicate predicate = pathPredicates.apply("/P*");
		MockServerRequest request = MockServerRequest.builder().uri(uri).build();
		assertThat(predicate.test(request)).isTrue();
	}

	@Test
	public void headers() {
		String name = "MyHeader";
		String value = "MyValue";
		RequestPredicate predicate =
				RequestPredicates.headers(
						headers -> headers.header(name).equals(Collections.singletonList(value)));
		MockServerRequest request = MockServerRequest.builder().header(name, value).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void contentType() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.contentType(json);
		MockServerRequest request = MockServerRequest.builder().header("Content-Type", json.toString()).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void accept() {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.accept(json);
		MockServerRequest request = MockServerRequest.builder().header("Accept", json.toString()).build();
		assertThat(predicate.test(request)).isTrue();

		request = MockServerRequest.builder().header("Accept", MediaType.TEXT_XML_VALUE).build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void pathExtension() {
		RequestPredicate predicate = RequestPredicates.pathExtension("txt");

		URI uri = URI.create("http://localhost/file.txt");
		MockServerRequest request = MockServerRequest.builder().uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		uri = URI.create("http://localhost/FILE.TXT");
		request = MockServerRequest.builder().uri(uri).build();
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.pathExtension("bar");
		assertThat(predicate.test(request)).isFalse();

		uri = URI.create("http://localhost/file.foo");
		request = MockServerRequest.builder().uri(uri).build();
		assertThat(predicate.test(request)).isFalse();
	}

	@Test
	public void queryParam() {
		MockServerRequest request = MockServerRequest.builder().queryParam("foo", "bar").build();
		RequestPredicate predicate = RequestPredicates.queryParam("foo", s -> s.equals("bar"));
		assertThat(predicate.test(request)).isTrue();

		predicate = RequestPredicates.queryParam("foo", s -> s.equals("baz"));
		assertThat(predicate.test(request)).isFalse();
	}

}
