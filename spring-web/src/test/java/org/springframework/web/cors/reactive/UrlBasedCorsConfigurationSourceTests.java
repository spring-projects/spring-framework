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

package org.springframework.web.cors.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UrlBasedCorsConfigurationSource}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class UrlBasedCorsConfigurationSourceTests {

	private final UrlBasedCorsConfigurationSource configSource
			= new UrlBasedCorsConfigurationSource();


	@Test
	public void empty() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bar/test.html"));
		assertThat(this.configSource.getCorsConfiguration(exchange)).isNull();
	}

	@Test
	public void registerAndMatch() {
		CorsConfiguration config = new CorsConfiguration();
		this.configSource.registerCorsConfiguration("/bar/**", config);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo/test.html"));
		assertThat(this.configSource.getCorsConfiguration(exchange)).isNull();

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bar/test.html"));
		assertThat(this.configSource.getCorsConfiguration(exchange)).isEqualTo(config);
	}

}
