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
 * Test a property definition by the using of {@link TestPropertySource} both in the
 * parent class and locally.
 *
 * @author Anatoliy Korovin
 * @since 5.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestPropertySource(properties = "key = 051187")
public class TestPropertySourceInheritTests extends ParentClassWithTestProperties {

	@Autowired
	Environment env;

	@Value("${key}")
	String key;

	@Value("${inherited}")
	String inherited;


	@Test
	public void inlinePropertyFromParentClassAndFromLocalTestPropertySourceAnnotation() {
		assertThat(env.getProperty("key")).isEqualTo("051187");
		assertThat(this.key).isEqualTo("051187");

		assertThat(env.getProperty("inherited")).isEqualTo("12345");
		assertThat(inherited).isEqualTo("12345");
	}


	@Configuration
	static class Config {
	}
}
