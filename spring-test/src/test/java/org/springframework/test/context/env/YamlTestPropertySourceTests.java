/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.env;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support
 * with a custom YAML {@link PropertySourceFactory}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
@YamlTestProperties("test-properties.yaml")
class YamlTestPropertySourceTests {

	@ParameterizedTest
	@CsvSource(delimiterString = "->", textBlock = """
			environments.dev.url   -> https://dev.example.com
			environments.dev.name  -> 'Developer Setup'
			environments.prod.url  -> https://prod.example.com
			environments.prod.name -> 'My Cool App'
			""")
	void propertyIsAvailableInEnvironment(String property, String value, @Autowired ConfigurableEnvironment env) {
		assertThat(env.getProperty(property)).isEqualTo(value);
	}


	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

}
