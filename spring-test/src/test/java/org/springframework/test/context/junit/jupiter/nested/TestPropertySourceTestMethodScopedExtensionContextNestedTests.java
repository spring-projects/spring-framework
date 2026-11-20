/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestPropertySource @TestPropertySource} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment with
 * {@link ExtensionContextScope#TEST_METHOD}.
 *
 * @author Sam Brannen
 * @since 6.2.13
 */
@SpringJUnitConfig
@TestPropertySource(properties = "p1 = v1")
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TestPropertySourceTestMethodScopedExtensionContextNestedTests {

	@Autowired
	Environment env1;


	@Test
	void propertiesInEnvironment() {
		assertThat(env1.getProperty("p1")).isEqualTo("v1");
	}


	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedCfgTests {

		@Autowired
		Environment env2;


		@Test
		void propertiesInEnvironment() {
			assertThat(env1).isSameAs(env2);
			assertThat(env2.getProperty("p1")).isEqualTo("v1");
		}
	}

	@Nested
	@SpringJUnitConfig(Config.class)
	@TestPropertySource(properties = "p2 = v2")
	class ConfigOverriddenByDefaultTests {

		@Autowired
		Environment env2;


		@Test
		void propertiesInEnvironment() {
			assertThat(env1).isSameAs(env2);
			assertThat(env2.getProperty("p1")).isNull();
			assertThat(env2.getProperty("p2")).isEqualTo("v2");
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	@TestPropertySource(properties = "p2a = v2a")
	@TestPropertySource(properties = "p2b = v2b")
	class InheritedAndExtendedCfgTests {

		@Autowired
		Environment env2;


		@Test
		void propertiesInEnvironment() {
			assertThat(env1).isSameAs(env2);
			assertThat(env2.getProperty("p1")).isEqualTo("v1");
			assertThat(env2.getProperty("p2a")).isEqualTo("v2a");
			assertThat(env2.getProperty("p2b")).isEqualTo("v2b");
		}


		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(Config.class)
		@TestPropertySource(properties = "p3 = v3")
		class L3OverriddenCfgTests {

			@Autowired
			Environment env3;


			@Test
			void propertiesInEnvironment() {
				assertThat(env1).isSameAs(env2).isSameAs(env3);
				assertThat(env3.getProperty("p1")).isNull();
				assertThat(env3.getProperty("p2")).isNull();
				assertThat(env3.getProperty("p3")).isEqualTo("v3");
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			@TestPropertySource(properties = {"p3 = v34", "p4 = v4"}, inheritProperties = false)
			class L4InheritedCfgButOverriddenTestPropsTests {

				@Autowired
				Environment env4;


				@Test
				void propertiesInEnvironment() {
					assertThat(env1).isSameAs(env2).isSameAs(env3).isSameAs(env4);
					assertThat(env4.getProperty("p1")).isNull();
					assertThat(env4.getProperty("p2")).isNull();
					assertThat(env4.getProperty("p3")).isEqualTo("v34");
					assertThat(env4.getProperty("p4")).isEqualTo("v4");
				}

				@Nested
				class L5InheritedCfgAndTestInterfaceTests implements TestInterface {

					@Autowired
					Environment env5;


					@Test
					void propertiesInEnvironment() {
						assertThat(env4).isSameAs(env5);
						assertThat(env5.getProperty("p1")).isNull();
						assertThat(env5.getProperty("p2")).isNull();
						assertThat(env5.getProperty("p3")).isEqualTo("v34");
						assertThat(env5.getProperty("p4")).isEqualTo("v4");
						assertThat(env5.getProperty("foo")).isEqualTo("bar");
						assertThat(env5.getProperty("enigma")).isEqualTo("42");
					}
				}
			}
		}
	}

	// -------------------------------------------------------------------------

	@Configuration
	static class Config {
		/* no user beans required for these tests */
	}

	@TestPropertySource(properties = { "foo = bar", "enigma: 42" })
	interface TestInterface {
	}

}
