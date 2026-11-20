/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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


	@Configuration(proxyBeanMethods = false)
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
