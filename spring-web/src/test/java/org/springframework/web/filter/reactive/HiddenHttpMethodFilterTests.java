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

package org.springframework.web.filter.reactive;

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HiddenHttpMethodFilter}
 *
 * @author Greg Turnquist
 */
public class HiddenHttpMethodFilterTests {

	private final HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();

	@Test
	public void filterWithParameter() {
		ServerWebExchange mockExchange = createExchange(Optional.of("DELETE"));

		WebFilterChain filterChain = exchange -> {
			assertEquals("Invalid method", HttpMethod.DELETE, exchange.getRequest().getMethod());
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(mockExchange, filterChain))
				.expectComplete()
				.verify();
	}

	@Test
	public void filterWithInvalidParameter() {
		ServerWebExchange mockExchange = createExchange(Optional.of("INVALID"));

		WebFilterChain filterChain = exchange -> Mono.empty();

		StepVerifier.create(filter.filter(mockExchange, filterChain))
				.consumeErrorWith(error -> {
					assertThat(error, Matchers.instanceOf(IllegalArgumentException.class));
					assertEquals(error.getMessage(), "HttpMethod 'INVALID' is not supported");
				})
				.verify();
	}

	@Test
	public void filterWithNoParameter() {
		ServerWebExchange mockExchange = createExchange(Optional.empty());

		WebFilterChain filterChain = exchange -> {
			assertEquals("Invalid method", HttpMethod.POST, exchange.getRequest().getMethod());
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(mockExchange, filterChain))
				.expectComplete()
				.verify();
	}

	@Test
	public void filterWithEmptyStringParameter() {
		ServerWebExchange mockExchange = createExchange(Optional.of(""));

		WebFilterChain filterChain = exchange -> {
			assertEquals("Invalid method", HttpMethod.POST, exchange.getRequest().getMethod());
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(mockExchange, filterChain))
				.expectComplete()
				.verify();
	}

	@Test
	public void filterWithDifferentMethodParam() {
		ServerWebExchange mockExchange = createExchange("_foo", Optional.of("DELETE"));

		WebFilterChain filterChain = exchange -> {
			assertEquals("Invalid method", HttpMethod.DELETE, exchange.getRequest().getMethod());
			return Mono.empty();
		};

		filter.setMethodParam("_foo");

		StepVerifier.create(filter.filter(mockExchange, filterChain))
				.expectComplete()
				.verify();
	}

	@Test
	public void filterWithoutPost() {
		ServerWebExchange mockExchange = createExchange(Optional.of("DELETE")).mutate()
				.request(builder -> builder.method(HttpMethod.PUT))
				.build();

		WebFilterChain filterChain = exchange -> {
			assertEquals("Invalid method", HttpMethod.PUT, exchange.getRequest().getMethod());
			return Mono.empty();
		};

		StepVerifier.create(filter.filter(mockExchange, filterChain))
				.expectComplete()
				.verify();
	}

	private ServerWebExchange createExchange(Optional<String> optionalMethod) {
		return createExchange("_method", optionalMethod);
	}

	private ServerWebExchange createExchange(String methodName, Optional<String> optionalBody) {
		MockServerHttpRequest.BodyBuilder builder = MockServerHttpRequest
				.post("/hotels")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);

		MockServerHttpRequest request = optionalBody
				.map(method -> builder.body(methodName + "=" + method))
				.orElse(builder.build());

		MockServerHttpResponse response = new MockServerHttpResponse();

		return new DefaultServerWebExchange(request, response);
	}

}
