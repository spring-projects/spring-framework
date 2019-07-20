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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for support {@link TestPropertySource @TestPropertySource} as a
 * repeatable annotation.
 *
 * Verify an overriding of a property value which defined both in custom annotation
 * and in the parent class, when this property declares locally by the
 * {@link TestPropertySource}.
 *
 * @author Anatoliy Korovin
 * @since 5.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@FooTestProperty
@TestPropertySource(properties = "foo = local value")
public class TestPropertySourceRepeatableOverridesTests extends FooTestPropertyDeclaration {

	@Value("${foo}")
	String foo;

	@Test
	public void inlinePropertyFromParentClassAndFromLocalTestPropertySourceAnnotation() {
		assertThat(foo).isEqualTo("local value");
	}

	@Configuration
	static class Config {
	}
}
