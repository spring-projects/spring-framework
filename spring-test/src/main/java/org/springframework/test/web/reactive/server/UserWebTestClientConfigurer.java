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

package org.springframework.test.web.reactive.server;

import java.security.cert.X509Certificate;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * {@link WebTestClientConfigurer} that modifies WebFlux mock server requests
 * by setting their {@link SslInfo}.
 *
 * <p>You can apply the configurer to a {@link WebTestClient.Builder}:
 *
 * <pre class="code">
 * WebTestClient client = webTestClientBuilder
 *         .apply(UserWebTestClientConfigurer.x509(certificate))
 *         .build();
 * </pre>
 *
 * <p>Or mutate an already built {@link WebTestClient}:
 *
 * <pre class="code">
 * WebTestClient newClient =
 *         client.mutateWith(UserWebTestClientConfigurer.x509(certificate));
 * </pre>
 *
 * <p><strong>Note:</strong> This configurer is applicable only to WebFlux mock
 * server setup. For a {@code WebTestClient.Builder} with a live server setup,
 * or a non-WebFlux, mock server, an {@link IllegalStateException} is raised.
 *
 * <p>For tests with a MockMvc server, refer to a similar facility to set the
 * user identity per request through Spring Security's
 * {@code SecurityMockMvcRequestPostProcessors}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public final class UserWebTestClientConfigurer implements WebTestClientConfigurer {

	private final SslInfo info;


	private UserWebTestClientConfigurer(SslInfo info) {
		this.info = info;
	}


	@Override
	public void afterConfigurerAdded(
			WebTestClient.Builder builder, @Nullable WebHttpHandlerBuilder httpHandlerBuilder,
			@Nullable ClientHttpConnector connector) {

		Assert.state(httpHandlerBuilder != null, "This configurer is applicable only to a mock WebFlux server");
		httpHandlerBuilder.filters(filters -> filters.add(0, new UserWebFilter()));
	}


	/**
	 * Create a configurer with the given {@link X509Certificate X509 certificate(s)}.
	 */
	public static UserWebTestClientConfigurer x509(X509Certificate... certificates) {
		return sslInfo(SslInfo.from("1", certificates));
	}

	/**
	 * Create a configurer with the given {@link SslInfo}.
	 */
	public static UserWebTestClientConfigurer sslInfo(SslInfo info) {
		return new UserWebTestClientConfigurer(info);
	}


	private final class UserWebFilter implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

			exchange = exchange.mutate()
					.request(builder -> builder.sslInfo(UserWebTestClientConfigurer.this.info))
					.build();

			return chain.filter(exchange);
		}
	}

}
