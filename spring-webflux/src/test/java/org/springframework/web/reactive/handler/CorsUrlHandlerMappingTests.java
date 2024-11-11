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

package org.springframework.web.reactive.handler;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CORS support at {@link AbstractUrlHandlerMapping} level.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
class CorsUrlHandlerMappingTests {

	private AbstractUrlHandlerMapping handlerMapping;

	private Object welcomeController = new Object();

	private CorsAwareHandler corsController = new CorsAwareHandler();


	@BeforeEach
	void setup() {
		this.handlerMapping = new AbstractUrlHandlerMapping() {};
		this.handlerMapping.registerHandler("/welcome.html", this.welcomeController);
		this.handlerMapping.registerHandler("/cors.html", this.corsController);
	}


	@Test
	void actualRequestWithoutCorsConfigurationProvider() {
		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isSameAs(this.welcomeController);
	}

	@Test
	void preflightRequestWithoutCorsConfigurationProvider() {
		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/welcome.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isNotSameAs(this.welcomeController);
	}

	@Test
	void actualRequestWithCorsAwareHandler() {
		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/cors.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isSameAs(this.corsController);
		assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
	}

	@Test
	void preFlightWithCorsAwareHandler() {
		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/cors.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isNotSameAs(this.corsController);
		assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
	}

	@Test
	void actualRequestWithGlobalCorsConfig() {
		CorsConfiguration mappedConfig = new CorsConfiguration();
		mappedConfig.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/welcome.html", mappedConfig));

		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isSameAs(this.welcomeController);
		assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
	}

	@Test
	void actualRequestWithGlobalPatternCorsConfig() {
		CorsConfiguration mappedConfig = new CorsConfiguration();
		mappedConfig.addAllowedOriginPattern("https://*.domain2.com");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/welcome.html", mappedConfig));

		String origin = "https://example.domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isSameAs(this.welcomeController);
		assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.isEqualTo("https://example.domain2.com");
	}

	@Test
	void preFlightRequestWithGlobalCorsConfig() {
		CorsConfiguration mappedConfig = new CorsConfiguration();
		mappedConfig.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/welcome.html", mappedConfig));

		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/welcome.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isNotSameAs(this.welcomeController);
		assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
	}

	@Test
	void actualRequestWithCorsConfigurationSource() {
		this.handlerMapping.setCorsConfigurationSource(new CustomCorsConfigurationSource());

		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isSameAs(this.welcomeController);
		assertThat(exchange.getResponse().getHeaders()
				.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(exchange.getResponse().getHeaders()
				.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
	}

	@Test
	void preFlightRequestWithCorsConfigurationSource() {
		this.handlerMapping.setCorsConfigurationSource(new CustomCorsConfigurationSource());

		String origin = "https://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/welcome.html", origin);
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertThat(actual).isNotNull();
		assertThat(actual).isNotSameAs(this.welcomeController);
		assertThat(exchange.getResponse().getHeaders()
				.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("https://domain2.com");
		assertThat(exchange.getResponse().getHeaders()
				.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
	}


	private ServerWebExchange createExchange(HttpMethod method, String path, String origin) {

		return MockServerWebExchange.from(MockServerHttpRequest
				.method(method, "http://localhost" + path)
				.header("Origin", origin)
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));
	}


	private class CorsAwareHandler implements CorsConfigurationSource {

		@Override
		public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("*");
			return config;
		}
	}

	public class CustomCorsConfigurationSource implements CorsConfigurationSource {

		@Override
		public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOriginPattern("*");
			config.setAllowCredentials(true);
			return config;
		}
	}

}
