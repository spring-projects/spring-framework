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

package org.springframework.test.context.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.Environment;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension.ExtensionContextScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.springframework.test.context.junit.jupiter.SpringExtension.EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME;
import static org.springframework.test.context.junit.jupiter.SpringExtension.ExtensionContextScope.TEST_CLASS;
import static org.springframework.test.context.junit.jupiter.SpringExtension.ExtensionContextScope.TEST_METHOD;

/**
 * Tests for {@link SpringExtension.ExtensionContextScope} and
 * {@link SpringExtension#EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME}.
 *
 * @author Sam Brannen
 * @since 7.0.7
 */
class SpringExtensionExtensionContextScopeTests {

	@Test
	void extensionContextScopeFromString() {
		assertThat(ExtensionContextScope.from(null)).isNull();
		assertThat(ExtensionContextScope.from("")).isNull();
		assertThat(ExtensionContextScope.from("   ")).isNull();
		assertThat(ExtensionContextScope.from("bogus")).isNull();

		assertThat(ExtensionContextScope.from("TEST_METHOD")).isSameAs(TEST_METHOD);
		assertThat(ExtensionContextScope.from("test_method")).isSameAs(TEST_METHOD);
		assertThat(ExtensionContextScope.from("Test_Method")).isSameAs(TEST_METHOD);

		assertThat(ExtensionContextScope.from("TEST_CLASS")).isSameAs(TEST_CLASS);
		assertThat(ExtensionContextScope.from("test_class")).isSameAs(TEST_CLASS);
		assertThat(ExtensionContextScope.from("Test_Class")).isSameAs(TEST_CLASS);
	}

	@Test
	void invalidExtensionContextScopeIsRejectedWhenConfiguredViaSpringProperties() {
		SpringProperties.setProperty(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, "bogus");
		try {
			EngineTestKit.engine("junit-jupiter")
					.selectors(selectClass(InvalidScopeTestCase.class))
					.execute()
					.testEvents()
					.assertStatistics(stats -> stats.started(1).failed(1));
		}
		finally {
			SpringProperties.setProperty(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, null);
		}
	}

	@Test
	void invalidExtensionContextScopeIsRejectedWhenConfiguredViaJUnitConfigurationParameter() {
		EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(InvalidScopeTestCase.class))
				.configurationParameter(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, "bogus")
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(1).failed(1));
	}

	@Test
	void testClassScopeConfiguredViaSpringProperties() {
		SpringProperties.setProperty(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, TEST_CLASS.name());
		try {
			var results = EngineTestKit.engine("junit-jupiter")
					.selectors(selectClass(GlobalClassScopedConfigurationTestCase.class))
					.execute();
			results.containerEvents()
					.assertStatistics(stats -> stats.started(3).succeeded(3).failed(0));
			results.testEvents()
					.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));
		}
		finally {
			SpringProperties.setProperty(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, null);
		}
	}

	@Test
	void testClassScopeConfiguredViaJUnitConfigurationParameter() {
		var results = EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(GlobalClassScopedConfigurationTestCase.class))
				.configurationParameter(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, TEST_CLASS.name())
				.execute();
		results.containerEvents()
				.assertStatistics(stats -> stats.started(3).succeeded(3).failed(0));
		results.testEvents()
				.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));
	}

	@Test
	void springExtensionConfigOverridesGlobalTestClassScopeConfiguration() {
		EngineTestKit.engine("junit-jupiter")
				.selectors(selectClass(SpringExtensionConfigOverridesGlobalPropertyTestCase.class))
				.configurationParameter(EXTENSION_CONTEXT_SCOPE_PROPERTY_NAME, TEST_CLASS.name())
				.execute()
				.testEvents()
				.assertStatistics(stats -> stats.started(2).succeeded(2).failed(0));
	}


	@SpringJUnitConfig
	static class InvalidScopeTestCase {

		@Test
		void test() {
			// no-op
		}
	}

	@SpringJUnitConfig
	@TestPropertySource(properties = "p1 = v1")
	@NestedTestConfiguration(OVERRIDE)
	@FailingTestCase
	static class GlobalClassScopedConfigurationTestCase {

		@Autowired
		Environment env1;

		@Test
		void propertiesInEnvironment() {
			assertThat(env1.getProperty("p1")).isEqualTo("v1");
		}

		@Nested
		@SpringJUnitConfig(Config.class)
		@TestPropertySource(properties = "p2 = v2")
		class ConfigOverriddenByDefaultTests {

			@Autowired
			Environment env2;

			@Test
			void propertiesInEnvironment() {
				assertThat(env1.getProperty("p1")).isEqualTo("v1");
				assertThat(env1).isNotSameAs(env2);
				assertThat(env2.getProperty("p1")).isNull();
				assertThat(env2.getProperty("p2")).isEqualTo("v2");
			}
		}

		@Configuration
		static class Config {
		}
	}

	@SpringJUnitConfig
	@SpringExtensionConfig(useTestClassScopedExtensionContext = false)
	@TestPropertySource(properties = "p1 = v1")
	@NestedTestConfiguration(OVERRIDE)
	static class SpringExtensionConfigOverridesGlobalPropertyTestCase {

		@Autowired
		Environment env1;

		@Test
		void propertiesInEnvironment() {
			assertThat(env1.getProperty("p1")).isEqualTo("v1");
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

		@Configuration
		static class Config {
		}
	}

}
