/*
 * Copyright 2002-2025 the original author or authors.
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

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultServerWebExchange}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class DefaultServerWebExchangeTests {

	@Test
	void transformUrlDefault() {
		ServerWebExchange exchange = createExchange();
		assertThat(exchange.transformUrl("/foo")).isEqualTo("/foo");
	}

	@Test
	void transformUrlWithEncoder() {
		ServerWebExchange exchange = createExchange();
		exchange.addUrlTransformer(s -> s + "?nonce=123");
		assertThat(exchange.transformUrl("/foo")).isEqualTo("/foo?nonce=123");
	}

	@Test
	void transformUrlWithMultipleEncoders() {
		ServerWebExchange exchange = createExchange();
		exchange.addUrlTransformer(s -> s + ";p=abc");
		exchange.addUrlTransformer(s -> s + "?q=123");
		assertThat(exchange.transformUrl("/foo")).isEqualTo("/foo;p=abc?q=123");
	}

	@Test // gh-34660
	void useFormDataMessageReaderWhenAllContentType() {
		MockServerHttpRequest request = MockServerHttpRequest
				.post("https://example.com")
				.header(HttpHeaders.CONTENT_TYPE, MediaType.ALL_VALUE)
				.body("project=spring");
		ServerWebExchange exchange = createExchange(request);
		MultiValueMap<String, String> body = exchange.getFormData().block();
		assertThat(body.get("project")).contains("spring");
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
