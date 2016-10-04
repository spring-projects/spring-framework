/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.cors.reactive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

/**
 * Unit tests for reactive {@link UrlBasedCorsConfigurationSource}.
 * @author Sebastien Deleuze
 */
public class UrlBasedCorsConfigurationSourceTests {

	private final UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();

	@Test
	public void empty() {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/bar/test.html");
		ServerWebExchange exchange = new DefaultServerWebExchange(request,
				new MockServerHttpResponse(), new MockWebSessionManager());
		assertNull(this.configSource.getCorsConfiguration(exchange));
	}

	@Test
	public void registerAndMatch() {
		CorsConfiguration config = new CorsConfiguration();
		this.configSource.registerCorsConfiguration("/bar/**", config);
		assertNull(this.configSource.getCorsConfiguration(
				new DefaultServerWebExchange(
						new MockServerHttpRequest(HttpMethod.GET, "/foo/test.html"),
						new MockServerHttpResponse(),
						new MockWebSessionManager())));
		assertEquals(config, this.configSource.getCorsConfiguration(new DefaultServerWebExchange(
						new MockServerHttpRequest(HttpMethod.GET, "/bar/test.html"),
						new MockServerHttpResponse(),
						new MockWebSessionManager())));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void unmodifiableConfigurationsMap() {
		this.configSource.getCorsConfigurations().put("/**", new CorsConfiguration());
	}

}
