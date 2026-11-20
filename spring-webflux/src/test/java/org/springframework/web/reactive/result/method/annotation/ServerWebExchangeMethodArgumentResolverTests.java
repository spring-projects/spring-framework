/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ServerWebExchangeMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class ServerWebExchangeMethodArgumentResolverTests {

	private final ServerWebExchangeMethodArgumentResolver resolver =
			new ServerWebExchangeMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private final MockServerWebExchange exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("https://example.org:9999/path?q=foo"));

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	void supportsParameter() {
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
	void resolveArgument() {
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
	void resolveUriComponentsBuilder() {
		MethodParameter param = this.testMethod.arg(UriComponentsBuilder.class);
		Object value = this.resolver.resolveArgument(param, new BindingContext(), this.exchange).block();

		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(UriComponentsBuilder.class);
		assertThat(((UriComponentsBuilder) value).path("/next").toUriString())
				.isEqualTo("https://example.org:9999/next");
	}

	@Test // gh-25822
	void resolveUriComponentsBuilderWithContextPath() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("https://example.org:9999/app/path?q=foo").contextPath("/app"));

		MethodParameter param = this.testMethod.arg(UriComponentsBuilder.class);
		Object value = this.resolver.resolveArgument(param, new BindingContext(), exchange).block();

		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(UriComponentsBuilder.class);
		assertThat(((UriComponentsBuilder) value).path("/next").toUriString())
				.isEqualTo("https://example.org:9999/app/next");
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
