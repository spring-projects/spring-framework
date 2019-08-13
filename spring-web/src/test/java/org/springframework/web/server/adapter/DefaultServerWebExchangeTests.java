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

package org.springframework.web.server.adapter;

import org.junit.jupiter.api.Test;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DefaultServerWebExchange}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class DefaultServerWebExchangeTests {

	@Test
	public void transformUrlDefault() {
		ServerWebExchange exchange = createExchange();
		assertThat(exchange.transformUrl("/foo")).isEqualTo("/foo");
	}

	@Test
	public void transformUrlWithEncoder() {
		ServerWebExchange exchange = createExchange();
		exchange.addUrlTransformer(s -> s + "?nonce=123");
		assertThat(exchange.transformUrl("/foo")).isEqualTo("/foo?nonce=123");
	}

	@Test
	public void transformUrlWithMultipleEncoders() {
		ServerWebExchange exchange = createExchange();
		exchange.addUrlTransformer(s -> s + ";p=abc");
		exchange.addUrlTransformer(s -> s + "?q=123");
		assertThat(exchange.transformUrl("/foo")).isEqualTo("/foo;p=abc?q=123");
	}


	private DefaultServerWebExchange createExchange() {
		MockServerHttpRequest request = MockServerHttpRequest.get("https://example.com").build();
		return createExchange(request);
	}

	private DefaultServerWebExchange createExchange(MockServerHttpRequest request) {
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(),
				new DefaultWebSessionManager(), ServerCodecConfigurer.create(),
				new AcceptHeaderLocaleContextResolver());
	}

}
