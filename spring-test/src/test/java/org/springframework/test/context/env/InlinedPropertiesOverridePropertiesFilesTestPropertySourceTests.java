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

package org.springframework.test.context.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource} support with
 * inlined properties that overrides properties files.
 *
 * @author Sam Brannen
 * @since 4.3
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@TestPropertySource(locations = "explicit.properties", properties = "explicit = inlined")
class InlinedPropertiesOverridePropertiesFilesTestPropertySourceTests {

	@Autowired
	Environment env;

	@Value("${explicit}")
	String explicit;


	@Test
	void inlinedPropertyOverridesValueFromPropertiesFile() {
		assertThat(env.getProperty("explicit")).isEqualTo("inlined");
		assertThat(this.explicit).isEqualTo("inlined");
	}


	@Configuration
	static class Config {
	}

}
