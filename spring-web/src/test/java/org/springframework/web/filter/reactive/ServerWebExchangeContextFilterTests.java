/*
 * Copyright 2002-present the original author or authors.
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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerWebExchangeContextFilter}.
 *
 * @author Rossen Stoyanchev
 */
class ServerWebExchangeContextFilterTests {


	@Test
	void extractServerWebExchangeFromContext() {
		MyService service = new MyService();

		WebHttpHandlerBuilder
				.webHandler(exchange -> service.service().then())
				.filter(new ServerWebExchangeContextFilter())
				.build()
				.handle(MockServerHttpRequest.get("/path").build(), new MockServerHttpResponse())
				.block(Duration.ofSeconds(5));

		assertThat(service.getExchange()).isNotNull();
	}


	private static class MyService {

		private final AtomicReference<ServerWebExchange> exchangeRef = new AtomicReference<>();

		public ServerWebExchange getExchange() {
			return this.exchangeRef.get();
		}

		public Mono<String> service() {
			return Mono.just("result")
					.transformDeferredContextual((mono, contextView) -> {
						ServerWebExchangeContextFilter.getExchange(contextView).ifPresent(exchangeRef::set);
						return mono;
					});
		}
	}

}
