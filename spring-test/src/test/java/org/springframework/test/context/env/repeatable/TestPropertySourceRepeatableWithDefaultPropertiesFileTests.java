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

package org.springframework.test.context.env.repeatable;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for support {@link TestPropertySource @TestPropertySource} as a
 * repeatable annotation.
 *
 * Verify a repeatable usage of {@link TestPropertySource} both with a default value of
 * properties file and with a specified properties file name in the
 * {@link TestPropertySource} annotation.
 *
 * @author Anatoliy Korovin
 * @since 5.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestPropertySource
@TestPropertySource("local.properties")
public class TestPropertySourceRepeatableWithDefaultPropertiesFileTests {

	@Autowired
	Environment env;

	@Value("${key}")
	String key;

	@Value("${default.value}")
	String defaultValue;

	@Test
	public void inlinePropertyFromParentClassAndFromLocalTestPropertySourceAnnotation() {
		assertPropertyValue("key", key, "local value");
		assertPropertyValue("default.value", defaultValue, "a value from default properties file");
	}

	private void assertPropertyValue(String name, String value, String expectedValue) {
		assertThat(env.getProperty(name)).isEqualTo(expectedValue);
		assertThat(value).isEqualTo(expectedValue);
	}


	@Configuration
	static class Config {
	}
}
