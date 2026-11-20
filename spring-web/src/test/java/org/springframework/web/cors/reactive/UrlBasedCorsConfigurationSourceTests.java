/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.cors.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UrlBasedCorsConfigurationSource}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
class UrlBasedCorsConfigurationSourceTests {

	private final UrlBasedCorsConfigurationSource configSource
			= new UrlBasedCorsConfigurationSource();


	@Test
	void empty() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bar/test.html"));
		assertThat(this.configSource.getCorsConfiguration(exchange)).isNull();
	}

	@Test
	void registerAndMatch() {
		CorsConfiguration config = new CorsConfiguration();
		this.configSource.registerCorsConfiguration("/bar/**", config);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo/test.html"));
		assertThat(this.configSource.getCorsConfiguration(exchange)).isNull();

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/bar/test.html"));
		assertThat(this.configSource.getCorsConfiguration(exchange)).isEqualTo(config);
	}

}
