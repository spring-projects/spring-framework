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

package org.springframework.web.reactive.result.method.annotation;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link ServerWebExchangeArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class ServerWebExchangeArgumentResolverTests {

	private final ServerWebExchangeArgumentResolver resolver =
			new ServerWebExchangeArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("http://example.org:9999/path?q=foo"));

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(ServerWebExchange.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(ServerHttpRequest.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(ServerHttpResponse.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(HttpMethod.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(Locale.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(TimeZone.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(ZoneId.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(UriComponentsBuilder.class)));
		assertTrue(this.resolver.supportsParameter(this.testMethod.arg(UriBuilder.class)));

		assertFalse(this.resolver.supportsParameter(this.testMethod.arg(String.class)));
		try {
			this.resolver.supportsParameter(this.testMethod.arg(Mono.class, ServerWebExchange.class));
			fail();
		}
		catch (IllegalStateException ex) {
			assertTrue("Unexpected error message:\n" + ex.getMessage(),
					ex.getMessage().startsWith(
							"ServerWebExchangeArgumentResolver doesn't support reactive type wrapper"));
		}
	}

	@Test
	public void resolveArgument() throws Exception {
		testResolveArgument(this.testMethod.arg(ServerWebExchange.class), this.exchange);
		testResolveArgument(this.testMethod.arg(ServerHttpRequest.class), this.exchange.getRequest());
		testResolveArgument(this.testMethod.arg(ServerHttpResponse.class), this.exchange.getResponse());
		testResolveArgument(this.testMethod.arg(HttpMethod.class), HttpMethod.GET);
		testResolveArgument(this.testMethod.arg(TimeZone.class), TimeZone.getDefault());
		testResolveArgument(this.testMethod.arg(ZoneId.class), ZoneId.systemDefault());
	}

	private void testResolveArgument(MethodParameter parameter, Object expected) {
		Mono<Object> mono = this.resolver.resolveArgument(parameter, new BindingContext(), this.exchange);
		assertEquals(expected, mono.block());
	}

	@Test
	public void resolveUriComponentsBuilder() throws Exception {
		MethodParameter param = this.testMethod.arg(UriComponentsBuilder.class);
		Object value = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block();

		assertNotNull(value);
		assertEquals(UriComponentsBuilder.class, value.getClass());
		assertEquals("http://example.org:9999/next", ((UriComponentsBuilder) value).path("/next").toUriString());
	}



	@SuppressWarnings("unused")
	public void handle(
			ServerWebExchange exchange,
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSession session,
			HttpMethod httpMethod,
			Locale locale,
			TimeZone timeZone,
			ZoneId zoneId,
			UriComponentsBuilder uriComponentsBuilder,
			UriBuilder uriBuilder,
			String s,
			Mono<ServerWebExchange> monoExchange) {
	}

}
