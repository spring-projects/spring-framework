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

package org.springframework.web.reactive.result.method.annotation;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link ServerWebExchangeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ServerWebExchangeMethodArgumentResolverTests {

	private final ServerWebExchangeMethodArgumentResolver resolver =
			new ServerWebExchangeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("https://example.org:9999/path?q=foo"));

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(ServerWebExchange.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(ServerHttpRequest.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(ServerHttpResponse.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(HttpMethod.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(Locale.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(TimeZone.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(ZoneId.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(UriComponentsBuilder.class))).isTrue();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(UriBuilder.class))).isTrue();

		assertThat(this.resolver.supportsParameter(this.testMethod.arg(WebSession.class))).isFalse();
		assertThat(this.resolver.supportsParameter(this.testMethod.arg(String.class))).isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.testMethod.arg(Mono.class, ServerWebExchange.class)))
			.withMessageStartingWith("ServerWebExchangeMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	public void resolveArgument() {
		testResolveArgument(this.testMethod.arg(ServerWebExchange.class), this.exchange);
		testResolveArgument(this.testMethod.arg(ServerHttpRequest.class), this.exchange.getRequest());
		testResolveArgument(this.testMethod.arg(ServerHttpResponse.class), this.exchange.getResponse());
		testResolveArgument(this.testMethod.arg(HttpMethod.class), HttpMethod.GET);
		testResolveArgument(this.testMethod.arg(TimeZone.class), TimeZone.getDefault());
		testResolveArgument(this.testMethod.arg(ZoneId.class), ZoneId.systemDefault());
	}

	private void testResolveArgument(MethodParameter parameter, Object expected) {
		Mono<Object> mono = this.resolver.resolveArgument(parameter, new BindingContext(), this.exchange);
		assertThat(mono.block()).isEqualTo(expected);
	}

	@Test
	public void resolveUriComponentsBuilder() {
		MethodParameter param = this.testMethod.arg(UriComponentsBuilder.class);
		Object value = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block();

		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(UriComponentsBuilder.class);
		assertThat(((UriComponentsBuilder) value).path("/next").toUriString()).isEqualTo("https://example.org:9999/next");
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
