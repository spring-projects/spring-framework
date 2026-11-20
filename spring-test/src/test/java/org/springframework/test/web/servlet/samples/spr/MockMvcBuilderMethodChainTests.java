/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.spr;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Test for SPR-10277 (multiple method chaining when building MockMvc).
 *
 * @author Wesley Hall
 */
@SpringJUnitWebConfig
class MockMvcBuilderMethodChainTests {

	@Test
	void chainMultiple(WebApplicationContext wac) {
		assertThatNoException().isThrownBy(() ->
			MockMvcBuilders
				.webAppContextSetup(wac)
				.addFilter(new CharacterEncodingFilter() )
				.defaultRequest(get("/").contextPath("/mywebapp"))
				.build()
			);
	}

	@Configuration
	@EnableWebMvc
	static class WebConfig {
	}

}
