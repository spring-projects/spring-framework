/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.filter.reactive;

import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HiddenHttpMethodFilter}.
 * @author Greg Turnquist
 * @author Rossen Stoyanchev
 */
public class HiddenHttpMethodFilterTests {

	private final HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();

	private final TestWebFilterChain filterChain = new TestWebFilterChain();


	@Test
	public void filterWithParameter() {
		postForm("_method=DELETE").block(Duration.ZERO);
		assertEquals(HttpMethod.DELETE, this.filterChain.getHttpMethod());
	}

	@Test
	public void filterWithParameterMethodNotAllowed() {
		postForm("_method=TRACE").block(Duration.ZERO);
		assertEquals(HttpMethod.POST, this.filterChain.getHttpMethod());
	}

	@Test
	public void filterWithNoParameter() {
		postForm("").block(Duration.ZERO);
		assertEquals(HttpMethod.POST, this.filterChain.getHttpMethod());
	}

	@Test
	public void filterWithEmptyStringParameter() {
		postForm("_method=").block(Duration.ZERO);
		assertEquals(HttpMethod.POST, this.filterChain.getHttpMethod());
	}

	@Test
	public void filterWithDifferentMethodParam() {
		this.filter.setMethodParamName("_foo");
		postForm("_foo=DELETE").block(Duration.ZERO);
		assertEquals(HttpMethod.DELETE, this.filterChain.getHttpMethod());
	}

	@Test
	public void filterWithInvalidMethodValue() {
		StepVerifier.create(postForm("_method=INVALID"))
				.consumeErrorWith(error -> {
					assertThat(error, Matchers.instanceOf(IllegalArgumentException.class));
					assertEquals("HttpMethod 'INVALID' not supported", error.getMessage());
				})
				.verify();
	}

	@Test
	public void filterWithHttpPut() {

		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.put("/")
						.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
						.body("_method=DELETE"));

		this.filter.filter(exchange, this.filterChain).block(Duration.ZERO);
		assertEquals(HttpMethod.PUT, this.filterChain.getHttpMethod());
	}


	private Mono<Void> postForm(String body) {

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/")
						.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
						.body(body));

		return this.filter.filter(exchange, this.filterChain);
	}


	private static class TestWebFilterChain implements WebFilterChain {

		private HttpMethod httpMethod;


		public HttpMethod getHttpMethod() {
			return this.httpMethod;
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange) {
			this.httpMethod = exchange.getRequest().getMethod();
			return Mono.empty();
		}
	}

}
