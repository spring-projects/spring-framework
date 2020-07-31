/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.context.testfixture.jndi.SimpleNamingContextBuilder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for {@link StandardServletEnvironment}.
 *
 * @author Chris Beams
 * @since 3.1
 */
public class StandardServletEnvironmentTests {

	@Test
	public void propertySourceOrder() throws Exception {
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
