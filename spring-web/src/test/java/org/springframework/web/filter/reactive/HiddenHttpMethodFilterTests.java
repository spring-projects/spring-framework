/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.filter.reactive;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HiddenHttpMethodFilter}.
 * @author Greg Turnquist
 * @author Rossen Stoyanchev
 */
class HiddenHttpMethodFilterTests {

	private final HiddenHttpMethodFilter filter = new HiddenHttpMethodFilter();

	private final TestWebFilterChain filterChain = new TestWebFilterChain();


	@Test
	void filterWithParameter() {
		postForm("_method=DELETE").block(Duration.ZERO);
		assertThat(this.filterChain.getHttpMethod()).isEqualTo(HttpMethod.DELETE);
	}

	@Test
	void filterWithParameterMethodNotAllowed() {
		postForm("_method=TRACE").block(Duration.ZERO);
		assertThat(this.filterChain.getHttpMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	void filterWithNoParameter() {
		postForm("").block(Duration.ZERO);
		assertThat(this.filterChain.getHttpMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	void filterWithEmptyStringParameter() {
		postForm("_method=").block(Duration.ZERO);
		assertThat(this.filterChain.getHttpMethod()).isEqualTo(HttpMethod.POST);
	}

	@Test
	void filterWithDifferentMethodParam() {
		this.filter.setMethodParamName("_foo");
		postForm("_foo=DELETE").block(Duration.ZERO);
		assertThat(this.filterChain.getHttpMethod()).isEqualTo(HttpMethod.DELETE);
	}

	@Test
	void filterWithHttpPut() {

		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.put("/")
						.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
						.body("_method=DELETE"));

		this.filter.filter(exchange, this.filterChain).block(Duration.ZERO);
		assertThat(this.filterChain.getHttpMethod()).isEqualTo(HttpMethod.PUT);
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
