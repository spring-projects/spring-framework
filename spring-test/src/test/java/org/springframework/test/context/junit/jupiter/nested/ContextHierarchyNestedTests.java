/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.nested.ContextHierarchyNestedTests.ParentConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ContextHierarchy @ContextHierarchy} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy(@ContextConfiguration(classes = ParentConfig.class))
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ContextHierarchyNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";
	private static final String QUX = "qux";

	@Autowired
	String foo;

	@Autowired
	ApplicationContext context;


	@Test
	void topLevelTest() {
		assertThat(this.context).as("local ApplicationContext").isNotNull();
		assertThat(this.context.getParent()).as("parent ApplicationContext").isNull();

		assertThat(foo).isEqualTo(FOO);
	}

	@Nested
	@ContextConfiguration(classes = NestedConfig.class)
	class NestedTests {

		@Autowired
		String bar;

		@Autowired
		ApplicationContext context;


		@Test
		void nestedTest() throws Exception {
			assertThat(this.context).as("local ApplicationContext").isNotNull();
			assertThat(this.context.getParent()).as("parent ApplicationContext").isNull();

			// In contrast to nested test classes running in JUnit 4, the foo
			// field in the outer instance should have been injected from the
			// test ApplicationContext for the outer instance.
			assertThat(foo).isEqualTo(FOO);
			assertThat(this.bar).isEqualTo(BAR);
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	@ContextConfiguration(classes = Child1Config.class)
	class NestedTestCaseWithInheritedConfigTests {

		@Autowired
		String bar;

		@Autowired
		ApplicationContext context;


		@Test
		void nestedTest() throws Exception {
			assertThat(this.context).as("local ApplicationContext").isNotNull();
			assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();

			// Since the configuration is inherited, the foo field in the outer instance
			// and the bar field in the inner instance should both have been injected
			// from the test ApplicationContext for the outer instance.
			assertThat(foo).isEqualTo(FOO);
			assertThat(this.bar).isEqualTo(BAZ + 1);
			assertThat(this.context.getBean("foo", String.class)).as("child foo").isEqualTo(QUX + 1);
		}

		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@ContextHierarchy({
			@ContextConfiguration(classes = ParentConfig.class),
			@ContextConfiguration(classes = Child2Config.class)
		})
		class DoubleNestedTestCaseWithOverriddenConfigTests {

			@Autowired
			String bar;

			@Autowired
			ApplicationContext context;


			@Test
			void nestedTest() throws Exception {
				assertThat(this.context).as("local ApplicationContext").isNotNull();
				assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();

				assertThat(foo).isEqualTo(FOO);
				assertThat(this.bar).isEqualTo(BAZ + 2);
				assertThat(this.context.getBean("foo", String.class)).as("child foo").isEqualTo(QUX + 2);
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Autowired
				@Qualifier("foo")
				String localFoo;

				@Autowired
				String bar;

				@Autowired
				ApplicationContext context;


				@Test
				void nestedTest() throws Exception {
					assertThat(this.context).as("local ApplicationContext").isNotNull();
					assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();
					assertThat(this.context.getParent().getParent()).as("grandparent ApplicationContext").isNotNull();

					assertThat(foo).isEqualTo(FOO);
					assertThat(this.localFoo).isEqualTo("test interface");
					assertThat(this.bar).isEqualTo(BAZ + 2);
					assertThat(this.context.getParent().getBean("foo", String.class)).as("child foo").isEqualTo(QUX + 2);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class ParentConfig {

		@Bean
		String foo() {
			return FOO;
		}
	}

	@Configuration
	static class Child1Config {

		@Bean
		String foo() {
			return QUX + 1;
		}

		@Bean
		String bar() {
			return BAZ + 1;
		}
	}

	@Configuration
	static class Child2Config {

		@Bean
		String foo() {
			return QUX + 2;
		}

		@Bean
		String bar() {
			return BAZ + 2;
		}
	}

	@Configuration
	static class NestedConfig {

		@Bean
		String bar() {
			return BAR;
		}
	}

	@Configuration
	static class TestInterfaceConfig {

		@Bean
		String foo() {
			return "test interface";
		}
	}

	@ContextConfiguration(classes = TestInterfaceConfig.class)
	interface TestInterface {
	}

}
