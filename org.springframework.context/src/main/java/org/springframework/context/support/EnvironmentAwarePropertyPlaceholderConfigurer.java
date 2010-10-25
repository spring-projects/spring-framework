/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.context.support;

import java.util.LinkedList;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractPropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;


/**
 * TODO SPR-7508: document
 *
 * Local properties are added as a property source in any case. Precedence is based
 * on the value of the {@link #setLocalOverride(boolean) localOverride} property.
 *
 * @author Chris Beams
 * @since 3.1
 * @see PropertyPlaceholderConfigurer
 * @see EnvironmentAwarePropertyOverrideConfigurer
 */
public class EnvironmentAwarePropertyPlaceholderConfigurer
		extends AbstractPropertyPlaceholderConfigurer implements EnvironmentAware {

	private ConfigurableEnvironment environment;
	private Environment wrappedEnvironment;

	public void setEnvironment(Environment environment) {
		this.wrappedEnvironment = environment;
	}

	@Override
	protected PlaceholderResolver getPlaceholderResolver(Properties props) {
		return new PlaceholderResolver() {
			public String resolvePlaceholder(String placeholderName) {
				return environment.getProperty(placeholderName);
			}
		};
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Assert.notNull(this.wrappedEnvironment, "Environment must not be null. Did you call setEnvironment()?");
		environment = new AbstractEnvironment() { };

		LinkedList<PropertySource<?>> propertySources = environment.getPropertySources();
		EnvironmentPropertySource environmentPropertySource =
			new EnvironmentPropertySource("wrappedEnvironment", wrappedEnvironment);

		if (!this.localOverride) {
			propertySources.add(environmentPropertySource);
		}

		if (this.localProperties != null) {
			int cx=0;
			for (Properties localProps : this.localProperties) {
				propertySources.add(new PropertiesPropertySource("localProperties"+cx++, localProps));
			}
		}

		if (this.localOverride) {
			propertySources.add(environmentPropertySource);
		}

		super.postProcessBeanFactory(beanFactory);
	}

	static class EnvironmentPropertySource extends PropertySource<Environment> {

		public EnvironmentPropertySource(String name, Environment source) {
			super(name, source);
		}

		@Override
		public boolean containsProperty(String key) {
			return source.containsProperty(key);
		}

		@Override
		public String getProperty(String key) {
			return source.getProperty(key);
		}

		@Override
		public int size() {
			// TODO Auto-generated method stub
			return source.getPropertyCount();
		}
	}
}
