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

package org.springframework.context.support;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractPropertyPlaceholderConfigurer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;

/**
 * Specialization of {@link AbstractPropertyPlaceholderConfigurer}
 *
 * <p>Local properties are added as a property source in any case. Precedence is based
 * on the value of the {@link #setLocalOverride localOverride} property.
 *
 * @author Chris Beams
 * @since 3.1
 * @see AbstractPropertyPlaceholderConfigurer
 * @see PropertyPlaceholderConfigurer
 */
public class PropertySourcesPlaceholderConfigurer extends AbstractPropertyPlaceholderConfigurer
		implements EnvironmentAware {

	/**
	 * {@value} is the name given to the {@link PropertySource} for the set of
	 * {@linkplain #mergeProperties() merged properties} supplied to this configurer.
	 */
	public static final String LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME = "localProperties";

	private MutablePropertySources propertySources;

	private PropertyResolver propertyResolver;

	private Environment environment;


	/**
	 * {@inheritDoc}
	 * <p>{@code PropertySources} from this environment will be searched when replacing ${...} placeholders
	 * @see #setPropertySources
	 * @see #postProcessBeanFactory
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Customize the set of {@link PropertySources} to be used by this configurer.
	 * Setting this property indicates that environment property sources and local
	 * properties should be ignored.
	 * @see #postProcessBeanFactory
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = new MutablePropertySources(propertySources);
	}

	@Override
	protected PlaceholderResolver getPlaceholderResolver(Properties props) {
		return new PlaceholderResolver() {
			public String resolvePlaceholder(String placeholderName) {
				return propertyResolver.getProperty(placeholderName);
			}
		};
	}

	/**
	 * {@inheritDoc}
	 * <p>Processing occurs by replacing ${...} placeholders in bean definitions by resolving each
	 * against this configurer's set of {@link PropertySources}, which includes:
	 * <ul>
	 *   <li>all {@linkplain Environment#getPropertySources environment property sources}, if an
	 *       {@code Environment} {@linkplain #setEnvironment is present}
	 *   <li>{@linkplain #mergeProperties merged local properties}, if {@linkplain #setLocation any}
	 *       {@linkplain #setLocations have} {@linkplain #setProperties been}
	 *       {@linkplain #setPropertiesArray specified}
	 *   <li>any property sources set by calling {@link #setPropertySources}
	 * </ul>
	 * <p>If {@link #setPropertySources} is called, <strong>environment and local properties will be
	 * ignored</strong>. This method is designed to give the user fine-grained control over property
	 * sources, and once set, the configurer makes no assumptions about adding additional sources.
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertySources == null) {
			this.propertySources = new MutablePropertySources();
			if (this.environment != null) {
				this.propertySources.addAll(this.environment.getPropertySources());
			}
			try {
				PropertySource<?> localPropertySource =
					new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, this.mergeProperties());
				if (this.localOverride) {
					this.propertySources.addFirst(localPropertySource);
				} else {
					this.propertySources.addLast(localPropertySource);
				}
			}
			catch (IOException ex) {
				throw new BeanInitializationException("Could not load properties", ex);
			}
		}

		this.propertyResolver = new PropertySourcesPropertyResolver(this.propertySources);
		this.processProperties(beanFactory, this.propertyResolver.asProperties());
	}

}
