/*
 * Copyright 2002-2024 the original author or authors.
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

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.DefaultWebFilterChain;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link UrlHandlerFilter}.
 *
 * @author Rossen Stoyanchev
 */
public class UrlHandlerFilterTests {

	@Test
	void requestMutation() {
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/path/**").mutateRequest().build();

		String path = "/path/123";
		MockServerHttpRequest original = MockServerHttpRequest.get(path + "/").build();
		ServerWebExchange exchange = MockServerWebExchange.from(original);

		ServerHttpRequest actual = invokeFilter(filter, exchange);

		assertThat(actual).isNotNull().isNotSameAs(original);
		assertThat(actual.getPath().value()).isEqualTo(path);
	}

	@Test
	void redirect() {
		HttpStatus status = HttpStatus.PERMANENT_REDIRECT;
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/path/*").redirect(status).build();

		String path = "/path/123";
		MockServerHttpRequest original = MockServerHttpRequest.get(path + "/").build();
		ServerWebExchange exchange = MockServerWebExchange.from(original);

		assertThatThrownBy(() -> invokeFilter(filter, exchange))
				.hasMessageContaining("No argument value was captured");

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
		assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create(path));
	}

	@Test
	void noUrlHandling() {
		testNoUrlHandling("/path/**", "", "/path/123");
		testNoUrlHandling("/path/*", "", "/path/123");
		testNoUrlHandling("/**", "", "/"); // gh-33444
		testNoUrlHandling("/**", "/myApp", "/myApp/"); // gh-33565
	}

	private static void testNoUrlHandling(String pattern, String contextPath, String path) {

		// No request mutation
		UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler(pattern).mutateRequest().build();

		MockServerHttpRequest request = MockServerHttpRequest.get(path).contextPath(contextPath).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);

		ServerHttpRequest actual = invokeFilter(filter, exchange);

		assertThat(actual).isNotNull().isSameAs(request);
		assertThat(actual.getPath().value()).isEqualTo(path);

		// No redirect
		HttpStatus status = HttpStatus.PERMANENT_REDIRECT;
		filter = UrlHandlerFilter.trailingSlashHandler(pattern).redirect(status).build();

		request = MockServerHttpRequest.get(path).contextPath(contextPath).build();
		exchange = MockServerWebExchange.from(request);

		actual = invokeFilter(filter, exchange);

		assertThat(actual).isNotNull().isSameAs(request);
		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getLocation()).isNull();
	}

	private static ServerHttpRequest invokeFilter(UrlHandlerFilter filter, ServerWebExchange exchange) {
		WebHandler handler = mock(WebHandler.class);
		ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
		given(handler.handle(captor.capture())).willReturn(Mono.empty());

		WebFilterChain chain = new DefaultWebFilterChain(handler, List.of(filter));
		filter.filter(exchange, chain).block();

		return captor.getValue().getRequest();
	}

}
