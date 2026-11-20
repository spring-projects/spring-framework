/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which ensure that test-related property sources are not
 * registered by default.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
@DirtiesContext
class DefaultTestPropertySourcesIntegrationTests {

	@Autowired
	ConfigurableEnvironment env;


	@Test
	void ensureTestRelatedPropertySourcesAreNotRegisteredByDefault() {
		assertPropertySourceIsNotRegistered(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME);
		assertPropertySourceIsNotRegistered(DynamicValuesPropertySource.PROPERTY_SOURCE_NAME);
	}

	private void assertPropertySourceIsNotRegistered(String name) {
		MutablePropertySources propertySources = this.env.getPropertySources();
		assertThat(propertySources.contains(name))
				.as("PropertySource \"%s\" should not be registered by default", name)
				.isFalse();
	}


	@Configuration
	static class Config {
	}

}
