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

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.ParameterizedCtxConfigTestMethodScopedExtensionContextNestedTests.TopLevelConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Parameterized variant of {@link ContextConfigurationTestMethodScopedExtensionContextNestedTests}
 * which tests {@link ParameterizedClass @ParameterizedClass} support.
 *
 * @author Sam Brannen
 * @since 7.0
 */
@SpringJUnitConfig(TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
@ParameterizedClass
@ValueSource(strings = {"foo", "bar"})
class ParameterizedCtxConfigTestMethodScopedExtensionContextNestedTests {

	private static final String FOO = "foo";
	private static final String BAR = "bar";
	private static final String BAZ = "baz";

	@Parameter
	String beanName;

	@Autowired
	ApplicationContext context;

	@Autowired(required = false)
	@Qualifier("foo")
	String foo;


	@Test
	void topLevelTest() {
		assertThat(foo).isEqualTo(FOO);
		if (beanName.equals(FOO)) {
			assertThat(context.getBean(beanName, String.class)).isEqualTo(beanName);
		}
	}


	@Nested
	@SpringJUnitConfig(NestedConfig.class)
	class NestedTests {

		@Autowired
		ApplicationContext localContext;

		@Autowired(required = false)
		@Qualifier("foo")
		String localFoo;

		@Autowired
		String bar;


		@Test
		void test() {
			assertThat(foo).as("foo bean should not be present").isNull();
			assertThat(this.localFoo).as("local foo bean should not be present").isNull();
			assertThat(this.bar).isEqualTo(BAR);
			if (beanName.equals(BAR)) {
				assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
			}
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	class NestedTestsWithInheritedConfigTests {

		@Autowired
		ApplicationContext localContext;

		@Autowired(required = false)
		@Qualifier("foo")
		String localFoo;

		@Autowired
		String bar;


		@Test
		void test() {
			// Since the configuration is inherited, the foo field in the outer instance
			// and the bar field in the inner instance should both have been injected
			// from the test ApplicationContext for the outer instance.
			assertThat(foo).isEqualTo(FOO);
			assertThat(this.localFoo).isEqualTo(FOO);
			assertThat(this.bar).isEqualTo(FOO);
			if (beanName.equals(FOO)) {
				assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
			}
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(NestedConfig.class)
		class DoubleNestedWithOverriddenConfigTests {

			@Autowired
			ApplicationContext localContext;

			@Autowired(required = false)
			@Qualifier("foo")
			String localFoo;

			@Autowired
			String bar;


			@Test
			void test() {
				assertThat(foo).as("foo bean should not be present").isNull();
				assertThat(this.localFoo).as("local foo bean should not be present").isNull();
				assertThat(this.bar).isEqualTo(BAR);
				if (beanName.equals(BAR)) {
					assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
				}
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigTests {

				@Autowired
				ApplicationContext localContext;

				@Autowired(required = false)
				@Qualifier("foo")
				String localFoo;

				@Autowired
				String bar;


				@Test
				void test() {
					assertThat(foo).as("foo bean should not be present").isNull();
					assertThat(this.localFoo).as("local foo bean should not be present").isNull();
					assertThat(this.bar).isEqualTo(BAR);
					if (beanName.equals(BAR)) {
						assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
					}
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Autowired
				ApplicationContext localContext;

				@Autowired(required = false)
				@Qualifier("foo")
				String localFoo;

				@Autowired
				@Qualifier("bar")
				String bar;

				@Autowired
				String baz;


				@Test
				void test() {
					assertThat(foo).as("foo bean should not be present").isNull();
					assertThat(this.localFoo).as("local foo bean should not be present").isNull();
					assertThat(this.bar).isEqualTo(BAR);
					assertThat(this.baz).isEqualTo(BAZ);
					if (beanName.equals(BAR) || beanName.equals(BAZ)) {
						assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
					}
				}
			}
		}
	}


	@Configuration(proxyBeanMethods = false)
	static class TopLevelConfig {

		@Bean
		String foo() {
			return FOO;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class NestedConfig {

		@Bean
		String bar() {
			return BAR;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class TestInterfaceConfig {

		@Bean
		String baz() {
			return BAZ;
		}
	}

	@ContextConfiguration(classes = TestInterfaceConfig.class)
	interface TestInterface {
	}

}
