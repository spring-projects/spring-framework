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

import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for {@link UrlBasedCorsConfigurationSource}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class UrlBasedCorsConfigurationSourceTests {

	private final UrlBasedCorsConfigurationSource configSource
			= new UrlBasedCorsConfigurationSource(new PathPatternParser());


	@Test
	public void empty() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bar/test.html"));
		assertNull(this.configSource.getCorsConfiguration(exchange));
	}

	@Test
	public void registerAndMatch() {
		CorsConfiguration config = new CorsConfiguration();
		this.configSource.registerCorsConfiguration("/bar/**", config);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo/test.html"));
		assertNull(this.configSource.getCorsConfiguration(exchange));

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bar/test.html"));
		assertEquals(config, this.configSource.getCorsConfiguration(exchange));
	}

}
