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

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit.jupiter.nested.WebAppConfigurationNestedTests.Config;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link WebAppConfiguration @WebAppConfiguration} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.0
 * @see ConstructorInjectionNestedTests
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
@SpringJUnitWebConfig(Config.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class WebAppConfigurationNestedTests {

	@Test
	void test(ApplicationContext context) {
		assertThat(context).isInstanceOf(WebApplicationContext.class);
	}


	@Nested
	@SpringJUnitConfig(Config.class)
	class ConfigOverriddenByDefaultTests {

		@Test
		void test(ApplicationContext context) {
			assertThat(context).isNotInstanceOf(WebApplicationContext.class);
		}
	}

	@Nested
	@SpringJUnitWebConfig(Config.class)
	class ConfigOverriddenByDefaultWebTests {

		@Test
		void test(ApplicationContext context) {
			assertThat(context).isInstanceOf(WebApplicationContext.class);
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	class NestedWithInheritedConfigTests {

		@Test
		void test(ApplicationContext context) {
			assertThat(context).isInstanceOf(WebApplicationContext.class);
		}


		@Nested
		class DoubleNestedWithImplicitlyInheritedConfigWebTests {

			@Test
			void test(ApplicationContext context) {
				assertThat(context).isInstanceOf(WebApplicationContext.class);
			}
		}

		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(Config.class)
		class DoubleNestedWithOverriddenConfigWebTests {

			@Test
			void test(ApplicationContext context) {
				assertThat(context).isNotInstanceOf(WebApplicationContext.class);
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigWebTests {

				@Test
				void test(ApplicationContext context) {
					assertThat(context).isNotInstanceOf(WebApplicationContext.class);
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Test
				void test(ApplicationContext context) {
					assertThat(context).isInstanceOf(WebApplicationContext.class);
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class Config {
	}

	@WebAppConfiguration
	interface TestInterface {
	}

}
