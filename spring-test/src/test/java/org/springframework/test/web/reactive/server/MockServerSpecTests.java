/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.cglib.core.internal.Function;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractMockServerSpec}.
 * @author Rossen Stoyanchev
 */
class MockServerSpecTests {

	private final TestMockServerSpec serverSpec = new TestMockServerSpec();


	@Test
	void applyFiltersAfterConfigurerAdded() {

		MockServerConfigurer configurer = new MockServerConfigurer() {

			@Override
			public void afterConfigureAdded(WebTestClient.MockServerSpec<?> spec) {
				spec.webFilter(new TestWebFilter("B"));
			}
		};

		this.serverSpec
				.webFilter(new TestWebFilter("A"))
				.apply(configurer)
				.build()
				.get().uri("/")
				.exchange()
				.expectBody(String.class)
				.consumeWith(result -> {
					String body = result.getResponseBody();
					assertThat(body).contains("test-attribute=:A:B");
				});
	}

	@Test
	void applyFiltersBeforeServerCreated() {

		MockServerConfigurer configurer = new MockServerConfigurer() {

			@Override
			public void beforeServerCreated(WebHttpHandlerBuilder builder) {
				builder.filters(filters -> {
					filters.add(0, new TestWebFilter("Fwk-A"));
					filters.add(1, new TestWebFilter("Fwk-B"));
				});
			}
		};

		this.serverSpec
				.webFilter(new TestWebFilter("App-A"))
				.webFilter(new TestWebFilter("App-B"))
				.apply(configurer)
				.build()
				.get().uri("/").exchange()
				.expectBody(String.class)
				.consumeWith(result -> {
					String body = result.getResponseBody();
					assertThat(body).contains("test-attribute=:Fwk-A:Fwk-B:App-A:App-B");
				});
	}

	@Test
	void sslInfo() {
		testSslInfo(info -> this.serverSpec.sslInfo(info).build());
	}

	@Test
	void sslInfoViaWebTestClientConfigurer() {
		testSslInfo(info -> this.serverSpec.configureClient().apply(UserWebTestClientConfigurer.sslInfo(info)).build());
	}

	@Test
	void sslInfoViaMutate() {
		testSslInfo(info -> this.serverSpec.build().mutateWith(UserWebTestClientConfigurer.sslInfo(info)));
	}

	private void testSslInfo(Function<SslInfo, WebTestClient> function) {
		SslInfo info = SslInfo.from("123");
		function.apply(info).get().uri("/").exchange().expectStatus().isOk();

		SslInfo actual = this.serverSpec.getSavedSslInfo();
		assertThat(actual).isSameAs(info);
	}


	private static class TestMockServerSpec extends AbstractMockServerSpec<TestMockServerSpec> {

		private @Nullable SslInfo savedSslInfo;

		public @Nullable SslInfo getSavedSslInfo() {
			return this.savedSslInfo;
		}

		@Override
		protected WebHttpHandlerBuilder initHttpHandlerBuilder() {
			return WebHttpHandlerBuilder.webHandler(exchange -> {
				this.savedSslInfo = exchange.getRequest().getSslInfo();
				DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;
				String text = exchange.getAttributes().toString();
				DataBuffer buffer = factory.wrap(text.getBytes(StandardCharsets.UTF_8));
				return exchange.getResponse().writeWith(Mono.just(buffer));
			});
		}
	}


	private record TestWebFilter(String name) implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			String name = "test-attribute";
			String value = exchange.getAttributeOrDefault(name, "");
			exchange.getAttributes().put(name, value + ":" + this.name);
			return chain.filter(exchange);
		}
	}

}
