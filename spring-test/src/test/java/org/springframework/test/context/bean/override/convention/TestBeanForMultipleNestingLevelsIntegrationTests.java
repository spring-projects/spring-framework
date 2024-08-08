/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.convention;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TestBean} that use multiple levels of
 * {@link Nested @Nested} test classes.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
class TestBeanForMultipleNestingLevelsIntegrationTests {

	@TestBean(name = "field0", methodName = "testField0")
	String field0;

	static String testField0() {
		return "zero";
	}

	static String testField1() {
		return "one";
	}

	static String testField2() {
		return "two";
	}

	@Test
	void test() {
		assertThat(field0).isEqualTo("zero");
	}


	@Nested
	class NestedLevel1Tests {

		@TestBean(name = "field1", methodName = "testField1")
		String field1;

		@Test
		void test() {
			assertThat(field0).isEqualTo("zero");
			assertThat(field1).isEqualTo("one");
		}

		@Nested
		class NestedLevel2Tests {

			@TestBean(name = "field2", methodName = "testField2")
			String field2;

			@Test
			void test() {
				assertThat(field0).isEqualTo("zero");
				assertThat(field1).isEqualTo("one");
				assertThat(field2).isEqualTo("two");
			}

			@Nested
			class NestedLevel3Tests {

				@TestBean(name = "field3", methodName = "testField2")
				String localField2;

				// Local testField2() "hides" the method in the top-level enclosing class.
				static String testField2() {
					return "Local Two";
				}

				@Test
				void test() {
					assertThat(field0).isEqualTo("zero");
					assertThat(field1).isEqualTo("one");
					assertThat(field2).isEqualTo("two");
					assertThat(localField2).isEqualTo("Local Two");
				}
			}
		}
	}


	@Configuration
	static class Config {

		@Bean
		String field0() {
			return "replace me 0";
		}

		@Bean
		String field1() {
			return "replace me 1";
		}

		@Bean
		String field2() {
			return "replace me 2";
		}

		@Bean
		String field3() {
			return "replace me 3";
		}
	}

}
