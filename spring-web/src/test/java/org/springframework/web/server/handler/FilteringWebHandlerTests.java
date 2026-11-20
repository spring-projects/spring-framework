/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.handler;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FilteringWebHandler}.
 *
 * @author Rossen Stoyanchev
 */
class FilteringWebHandlerTests {

	private static final Log logger = LogFactory.getLog(FilteringWebHandlerTests.class);


	@Test
	void multipleFilters() {

		TestFilter filter1 = new TestFilter();
		TestFilter filter2 = new TestFilter();
		TestFilter filter3 = new TestFilter();
		StubWebHandler targetHandler = new StubWebHandler();

		new FilteringWebHandler(targetHandler, Arrays.asList(filter1, filter2, filter3))
				.handle(MockServerWebExchange.from(MockServerHttpRequest.get("/")))
				.block(Duration.ZERO);

		assertThat(filter1.invoked()).isTrue();
		assertThat(filter2.invoked()).isTrue();
		assertThat(filter3.invoked()).isTrue();
		assertThat(targetHandler.invoked()).isTrue();
	}

	@Test
	void zeroFilters() {

		StubWebHandler targetHandler = new StubWebHandler();

		new FilteringWebHandler(targetHandler, Collections.emptyList())
				.handle(MockServerWebExchange.from(MockServerHttpRequest.get("/")))
				.block(Duration.ZERO);

		assertThat(targetHandler.invoked()).isTrue();
	}

	@Test
	void shortcircuitFilter() {

		TestFilter filter1 = new TestFilter();
		ShortcircuitingFilter filter2 = new ShortcircuitingFilter();
		TestFilter filter3 = new TestFilter();
		StubWebHandler targetHandler = new StubWebHandler();

		new FilteringWebHandler(targetHandler, Arrays.asList(filter1, filter2, filter3))
				.handle(MockServerWebExchange.from(MockServerHttpRequest.get("/")))
				.block(Duration.ZERO);

		assertThat(filter1.invoked()).isTrue();
		assertThat(filter2.invoked()).isTrue();
		assertThat(filter3.invoked()).isFalse();
		assertThat(targetHandler.invoked()).isFalse();
	}

	@Test
	void asyncFilter() {

		AsyncFilter filter = new AsyncFilter();
		StubWebHandler targetHandler = new StubWebHandler();

		new FilteringWebHandler(targetHandler, Collections.singletonList(filter))
				.handle(MockServerWebExchange.from(MockServerHttpRequest.get("/")))
				.block(Duration.ofSeconds(5));

		assertThat(filter.invoked()).isTrue();
		assertThat(targetHandler.invoked()).isTrue();
	}

	@Test
	void handleErrorFromFilter() {

		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerHttpResponse response = new MockServerHttpResponse();

		TestExceptionHandler exceptionHandler = new TestExceptionHandler();

		WebHttpHandlerBuilder.webHandler(new StubWebHandler())
				.filter(new ExceptionFilter())
				.exceptionHandler(exceptionHandler).build()
				.handle(request, response)
				.block();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(exceptionHandler.ex).isNotNull();
		assertThat(exceptionHandler.ex.getMessage()).isEqualTo("boo");
	}


	private static class TestFilter implements WebFilter {

		private volatile boolean invoked;

		public boolean invoked() {
			return this.invoked;
		}

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			this.invoked = true;
			return doFilter(exchange, chain);
		}

		public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
			return chain.filter(exchange);
		}
	}


	private static class ShortcircuitingFilter extends TestFilter {

		@Override
		public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
			return Mono.empty();
		}
	}


	private static class AsyncFilter extends TestFilter {

		@Override
		public Mono<Void> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
			return doAsyncWork().flatMap(asyncResult -> {
				logger.debug("Async result: " + asyncResult);
				return chain.filter(exchange);
			});
		}

		private Mono<String> doAsyncWork() {
			return Mono.delay(Duration.ofMillis(100L)).map(l -> "123");
		}
	}


	private static class ExceptionFilter implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			return Mono.error(new IllegalStateException("boo"));
		}
	}


	private static class TestExceptionHandler implements WebExceptionHandler {

		private Throwable ex;

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			this.ex = ex;
			return Mono.error(ex);
		}
	}


	private static class StubWebHandler implements WebHandler {

		private volatile boolean invoked;

		public boolean invoked() {
			return this.invoked;
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			logger.trace("StubHandler invoked.");
			this.invoked = true;
			return Mono.empty();
		}
	}

}
