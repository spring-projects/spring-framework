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

package org.springframework.web.reactive.function;

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
		MockRequest request = MockRequest.builder().build();
		assertTrue(predicate.test(request));
	}

	@Test
	public void method() throws Exception {
		HttpMethod httpMethod = HttpMethod.GET;
		RequestPredicate predicate = RequestPredicates.method(httpMethod);
		MockRequest request = MockRequest.builder().method(httpMethod).build();
		assertTrue(predicate.test(request));

		request = MockRequest.builder().method(HttpMethod.POST).build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void methods() throws Exception {
		URI uri = URI.create("http://localhost/path");

		RequestPredicate predicate = RequestPredicates.GET("/p*");
		MockRequest request = MockRequest.builder().method(HttpMethod.GET).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.HEAD("/p*");
		request = MockRequest.builder().method(HttpMethod.HEAD).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.POST("/p*");
		request = MockRequest.builder().method(HttpMethod.POST).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.PUT("/p*");
		request = MockRequest.builder().method(HttpMethod.PUT).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.PATCH("/p*");
		request = MockRequest.builder().method(HttpMethod.PATCH).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.DELETE("/p*");
		request = MockRequest.builder().method(HttpMethod.DELETE).uri(uri).build();
		assertTrue(predicate.test(request));

		predicate = RequestPredicates.OPTIONS("/p*");
		request = MockRequest.builder().method(HttpMethod.OPTIONS).uri(uri).build();
		assertTrue(predicate.test(request));

	}

	@Test
	public void path() throws Exception {
		URI uri = URI.create("http://localhost/path");
		RequestPredicate predicate = RequestPredicates.path("/p*");
		MockRequest request = MockRequest.builder().uri(uri).build();
		assertTrue(predicate.test(request));

		request = MockRequest.builder().build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void headers() throws Exception {
		String name = "MyHeader";
		String value = "MyValue";
		RequestPredicate predicate =
				RequestPredicates.headers(headers -> {
					return headers.header(name).equals(
							Collections.singletonList(value));
				});
		MockRequest request = MockRequest.builder().header(name, value).build();
		assertTrue(predicate.test(request));

		request = MockRequest.builder().build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void contentType() throws Exception {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.contentType(json);
		MockRequest request = MockRequest.builder().header("Content-Type", json.toString()).build();
		assertTrue(predicate.test(request));

		request = MockRequest.builder().build();
		assertFalse(predicate.test(request));
	}

	@Test
	public void accept() throws Exception {
		MediaType json = MediaType.APPLICATION_JSON;
		RequestPredicate predicate = RequestPredicates.accept(json);
		MockRequest request = MockRequest.builder().header("Accept", json.toString()).build();
		assertTrue(predicate.test(request));

		request = MockRequest.builder().build();
		assertFalse(predicate.test(request));
	}

}