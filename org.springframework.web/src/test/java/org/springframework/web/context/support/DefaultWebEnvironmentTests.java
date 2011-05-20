/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.support;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.jndi.JndiPropertySource;

public class DefaultWebEnvironmentTests {

	@Test
	public void propertySourceOrder() {
		ConfigurableEnvironment env = new DefaultWebEnvironment();
		MutablePropertySources sources = env.getPropertySources();
		assertThat(sources.precedenceOf(PropertySource.named(DefaultWebEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)), equalTo(0));
		assertThat(sources.precedenceOf(PropertySource.named(DefaultWebEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)), equalTo(1));
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)), equalTo(2));
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)), equalTo(3));
		assertThat(sources.size(), is(4));
	}

	@Test
	public void propertySourceOrder_jndiPropertySourceEnabled() {
		System.setProperty(JndiPropertySource.JNDI_PROPERTY_SOURCE_ENABLED_FLAG, "true");
		ConfigurableEnvironment env = new DefaultWebEnvironment();
		MutablePropertySources sources = env.getPropertySources();

		assertThat(sources.precedenceOf(PropertySource.named(DefaultWebEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)), equalTo(0));
		assertThat(sources.precedenceOf(PropertySource.named(DefaultWebEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)), equalTo(1));
		assertThat(sources.precedenceOf(PropertySource.named(JndiPropertySource.JNDI_PROPERTY_SOURCE_NAME)), equalTo(2));
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)), equalTo(3));
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)), equalTo(4));
		assertThat(sources.size(), is(5));

		System.clearProperty(JndiPropertySource.JNDI_PROPERTY_SOURCE_ENABLED_FLAG);
	}

	@Test
	public void propertySourceOrder_jndiPropertySourceEnabledIsFalse() {
		System.setProperty(JndiPropertySource.JNDI_PROPERTY_SOURCE_ENABLED_FLAG, "false");
		ConfigurableEnvironment env = new DefaultWebEnvironment();
		MutablePropertySources sources = env.getPropertySources();

		assertThat(sources.precedenceOf(PropertySource.named(DefaultWebEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME)), equalTo(0));
		assertThat(sources.precedenceOf(PropertySource.named(DefaultWebEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME)), equalTo(1));
		//assertThat(sources.precedenceOf(PropertySource.named(JndiPropertySource.JNDI_PROPERTY_SOURCE_NAME)), equalTo(2));
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME)), equalTo(2));
		assertThat(sources.precedenceOf(PropertySource.named(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)), equalTo(3));
		assertThat(sources.size(), is(4));

		System.clearProperty(JndiPropertySource.JNDI_PROPERTY_SOURCE_ENABLED_FLAG);
	}
}
