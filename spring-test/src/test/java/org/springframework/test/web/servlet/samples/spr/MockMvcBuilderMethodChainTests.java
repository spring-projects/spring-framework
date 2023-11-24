/*
 * Copyright 2002-2022 the original author or authors.
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
