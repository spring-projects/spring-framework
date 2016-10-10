/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.handler;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Unit tests for CORS support at {@link AbstractUrlHandlerMapping} level.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CorsUrlHandlerMappingTests {

	private AbstractUrlHandlerMapping handlerMapping;

	private Object welcomeController = new Object();

	private CorsAwareHandler corsController = new CorsAwareHandler();


	@Before
	public void setup() {
		this.handlerMapping = new AbstractUrlHandlerMapping() {};
		this.handlerMapping.setUseTrailingSlashMatch(true);
		this.handlerMapping.registerHandler("/welcome.html", this.welcomeController);
		this.handlerMapping.registerHandler("/cors.html", this.corsController);
	}


	@Test
	public void actualRequestWithoutCorsConfigurationProvider() throws Exception {
		String origin = "http://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", origin, "GET");
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertNotNull(actual);
		assertSame(this.welcomeController, actual);
	}

	@Test
	public void preflightRequestWithoutCorsConfigurationProvider() throws Exception {
		String origin = "http://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/welcome.html", origin, "GET");
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertNotNull(actual);
		assertNotSame(this.welcomeController, actual);
		assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void actualRequestWithCorsAwareHandler() throws Exception {
		String origin = "http://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/cors.html", origin, "GET");
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertNotNull(actual);
		assertSame(this.corsController, actual);
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void preFlightWithCorsAwareHandler() throws Exception {
		String origin = "http://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/cors.html", origin, "GET");
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertNotNull(actual);
		assertNotSame(this.corsController, actual);
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void actualRequestWithGlobalCorsConfig() throws Exception {
		CorsConfiguration mappedConfig = new CorsConfiguration();
		mappedConfig.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/welcome.html", mappedConfig));

		String origin = "http://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", origin, "GET");
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertNotNull(actual);
		assertSame(this.welcomeController, actual);
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void preFlightRequestWithGlobalCorsConfig() throws Exception {
		CorsConfiguration mappedConfig = new CorsConfiguration();
		mappedConfig.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/welcome.html", mappedConfig));

		String origin = "http://domain2.com";
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/welcome.html", origin, "GET");
		Object actual = this.handlerMapping.getHandler(exchange).block();

		assertNotNull(actual);
		assertNotSame(this.welcomeController, actual);
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}


	private ServerWebExchange createExchange(HttpMethod method, String path, String origin,
			String accessControlRequestMethod) {

		ServerHttpRequest request = new MockServerHttpRequest(method, "http://localhost" + path);
		request.getHeaders().add(HttpHeaders.ORIGIN, origin);
		request.getHeaders().add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, accessControlRequestMethod);
		MockServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, response, sessionManager);
	}


	private class CorsAwareHandler implements CorsConfigurationSource {

		@Override
		public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("*");
			return config;
		}
	}

}
