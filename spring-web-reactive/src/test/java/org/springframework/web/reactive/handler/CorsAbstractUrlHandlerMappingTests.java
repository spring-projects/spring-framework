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

import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class CorsAbstractUrlHandlerMappingTests {

	private AnnotationConfigApplicationContext wac;

	private TestUrlHandlerMapping handlerMapping;

	private Object mainController;

	private CorsAwareHandler corsConfigurationSourceController;

	@Before
	public void setup() {
		wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();

		handlerMapping = (TestUrlHandlerMapping) wac.getBean("handlerMapping");
		mainController = wac.getBean("mainController");
		corsConfigurationSourceController = (CorsAwareHandler) wac.getBean("corsConfigurationSourceController");
	}

	@Test
	public void actualRequestWithoutCorsConfigurationProvider() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", "http://domain2.com", "GET");
		Object actual = handlerMapping.getHandler(exchange).block();
		assertNotNull(actual);
		assertSame(mainController, actual);
	}

	@Test
	public void preflightRequestWithoutCorsConfigurationProvider() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/welcome.html", "http://domain2.com", "GET");
		Object actual = handlerMapping.getHandler(exchange).block();
		assertNotNull(actual);
		assertEquals("NoOpHandler", actual.getClass().getSimpleName());
		assertNull(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void actualRequestWithCorsConfigurationProvider() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/cors.html", "http://domain2.com", "GET");
		Object actual = handlerMapping.getHandler(exchange).block();
		assertNotNull(actual);
		assertSame(corsConfigurationSourceController, actual);
		CorsConfiguration config = ((CorsConfigurationSource)actual).getCorsConfiguration(createExchange(HttpMethod.GET, "", "",""));
		assertNotNull(config);
		assertArrayEquals(config.getAllowedOrigins().toArray(), new String[]{"*"});
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void preflightRequestWithCorsConfigurationProvider() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/cors.html", "http://domain2.com", "GET");
		Object actual = handlerMapping.getHandler(exchange).block();
		assertNotNull(actual);
		assertEquals("NoOpHandler", actual.getClass().getSimpleName());
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void actualRequestWithMappedCorsConfiguration() throws Exception {
		CorsConfiguration mappedConfig = new CorsConfiguration();
		mappedConfig.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/welcome.html", mappedConfig));

		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/welcome.html", "http://domain2.com", "GET");
		Object actual = handlerMapping.getHandler(exchange).block();
		assertNotNull(actual);
		assertSame(mainController, actual);
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void preflightRequestWithMappedCorsConfiguration() throws Exception {
		CorsConfiguration mappedConfig = new CorsConfiguration();
		mappedConfig.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/welcome.html", mappedConfig));

		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, "/welcome.html", "http://domain2.com", "GET");
		Object actual = handlerMapping.getHandler(exchange).block();
		assertNotNull(actual);
		assertEquals("NoOpHandler", actual.getClass().getSimpleName());
		assertEquals("*", exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}


	private ServerWebExchange createExchange(HttpMethod method, String path, String origin,
			String accessControlRequestMethod) throws URISyntaxException {

		ServerHttpRequest request = new MockServerHttpRequest(method, "http://localhost" + path);
		request.getHeaders().add(HttpHeaders.ORIGIN, origin);
		request.getHeaders().add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, accessControlRequestMethod);
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}


	@Configuration
	static class WebConfig {
	
		@Bean @SuppressWarnings("unused")
		public TestUrlHandlerMapping handlerMapping() {
			TestUrlHandlerMapping hm = new TestUrlHandlerMapping();
			hm.setUseTrailingSlashMatch(true);
			hm.registerHandler("/welcome.html", mainController());
			hm.registerHandler("/cors.html", corsConfigurationSourceController());
			return hm;
		}

		@Bean
		public Object mainController() {
			return new Object();
		}

		@Bean
		public CorsAwareHandler corsConfigurationSourceController() {
			return new CorsAwareHandler();
		}

	}

	static class TestUrlHandlerMapping extends AbstractUrlHandlerMapping {

	}

	static class CorsAwareHandler implements CorsConfigurationSource {

		@Override
		public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("*");
			return config;
		}
	}

}
