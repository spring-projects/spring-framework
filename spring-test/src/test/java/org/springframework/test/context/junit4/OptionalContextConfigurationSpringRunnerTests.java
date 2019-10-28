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

package org.springframework.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based integration test which verifies that
 * {@link ContextConfiguration @ContextConfiguration} is optional.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
 */
@RunWith(SpringRunner.class)
public class OptionalContextConfigurationSpringRunnerTests {

	@Autowired
	String foo;


	@Test
	public void contextConfigurationAnnotationIsOptional() {
		assertThat(foo).isEqualTo("foo");
	}


	@Configuration
	static class Config {

		@Bean
		String foo() {
			return "foo";
		}
	}

}
