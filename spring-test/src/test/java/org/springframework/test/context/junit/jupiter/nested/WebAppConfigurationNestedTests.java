/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
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
 * @see ConstructorInjectionTestClassScopedExtensionContextNestedTests
 * @see ConstructorInjectionTestMethodScopedExtensionContextNestedTests
 * @see org.springframework.test.context.junit4.nested.NestedTestsWithSpringRulesTests
 */
@SpringJUnitWebConfig
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
