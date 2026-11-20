/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.jndi.SimpleNamingContextBuilder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link StandardServletEnvironment}.
 *
 * @author Chris Beams
 * @since 3.1
 */
class StandardServletEnvironmentTests {

	@Test
	void propertySourceOrder() throws Exception {
		SimpleNamingContextBuilder.emptyActivatedContextBuilder();

		ConfigurableEnvironment env = new StandardServletEnvironment();
		MutablePropertySources sources = env.getPropertySources();

		assertThat(sources.precedenceOf(PropertySource.named(
				StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME))).isEqualTo(0);
		assertThat(sources.precedenceOf(PropertySource.named(
				StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME))).isEqualTo(1);
		assertThat(sources.precedenceOf(PropertySource.named(
				StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME))).isEqualTo(2);
		assertThat(sources.precedenceOf(PropertySource.named(
				StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME))).isEqualTo(3);
		assertThat(sources.precedenceOf(PropertySource.named(
				StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))).isEqualTo(4);
		assertThat(sources).hasSize(5);
	}

}
