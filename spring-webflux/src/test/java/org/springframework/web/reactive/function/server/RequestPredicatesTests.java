/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
public class RequestPredicatesTests {

	@Test
	public void all() throws Exception {
		RequestPredicate predicate = RequestPredicates.all();
		MockServerRequest request = MockServerRequest.builder().build();
		assertTrue(predicate.test(request));
	}

	@Test
	public void method() throws Exception {
		HttpMethod httpMethod = HttpMethod.GET;
		RequestPredicate predicate = RequestPredicates.method(httpMethod);
		MockServerRequest request = MockServerRequest.builder().method(httpMethod).build();
		assertTrue(predicate.test(request));

		request = MockServerRequest.builder().method(HttpMethod.POST).build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void methods() throws Exception {
		URI uri = URI.create("http://localhost/path");

		RequestPredicate predicate = RequestPredicates.GET("/p*");
		MockServerRequest request = MockServerRequest.builder().method(HttpMethod.GET).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.HEAD("/p*");
		request = MockServerRequest.builder().method(HttpMethod.HEAD).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.POST("/p*");
		request = MockServerRequest.builder().method(HttpMethod.POST).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.PUT("/p*");
		request = MockServerRequest.builder().method(HttpMethod.PUT).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.PATCH("/p*");
		request = MockServerRequest.builder().method(HttpMethod.PATCH).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.DELETE("/p*");
		request = MockServerRequest.builder().method(HttpMethod.DELETE).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.OPTIONS("/p*");
		request = MockServerRequest.builder().method(HttpMethod.OPTIONS).uri(uri).build();
		assertTrue(predicate.test(request));
	}

	@Test
	public void path() throws Exception {
		URI uri = URI.create("http://localhost/path");
		RequestPredicate predicate = RequestPredicates.path("/p*");
		MockServerRequest request = MockServerRequest.builder().uri(uri).build();
		assertTrue(predicate.test(request));

		request = MockServerRequest.builder().build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void headers() throws Exception {
		String name = "MyHeader";
		String value = "MyValue";
		RequestPredicate predicate =
				RequestPredicates.headers(
						headers -> headers.header(name).equals(Collections.singletonList(value)));
		MockServerRequest request = MockServerRequest.builder().header(name, value).build();
		assertTrue(predicate.test(request));

		request = MockServerRequest.builder().build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void contentType() throws Exception {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.contentType(json);
		MockServerRequest request = MockServerRequest.builder().header("Content-Type", json.toString()).build();
		assertTrue(predicate.test(request));

		request = MockServerRequest.builder().build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void accept() throws Exception {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.accept(json);
		MockServerRequest request = MockServerRequest.builder().header("Accept", json.toString()).build();
		assertTrue(predicate.test(request));

		request = MockServerRequest.builder().build();
		assertFalse(predicate.test(request));
	}

}