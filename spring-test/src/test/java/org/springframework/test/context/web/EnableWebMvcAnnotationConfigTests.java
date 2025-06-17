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

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests used to assess claims raised in
 * <a href="https://github.com/spring-projects/spring-framework/issues/14432">gh-14432</a>.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see EnableWebMvcXmlConfigTests
 */
@SpringJUnitWebConfig
// NOTE: if we omit the @WebAppConfiguration declaration, the ApplicationContext will fail
// to load since @EnableWebMvc requires that the context be a WebApplicationContext.
class EnableWebMvcAnnotationConfigTests extends AbstractBasicWacTests {

	@Test
	void applicationContextLoads(WebApplicationContext wac) {
		assertThat(wac.getBean("foo", String.class)).isEqualTo("enigma");
	}


	@Configuration
	@EnableWebMvc
	static class Config {

		@Bean
		String foo() {
			return "enigma";
		}
	}

}
