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

package org.springframework.context.support;

import java.io.IOException;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.ConfigurablePropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

/**
 * Specialization of {@link PlaceholderConfigurerSupport} that resolves ${...} placeholders
 * within bean definition property values and {@code @Value} annotations against the current
 * Spring {@link Environment} and its set of {@link PropertySources}.
 *
 * <p>This class is designed as a general replacement for {@code PropertyPlaceholderConfigurer}.
 * It is used by default to support the {@code property-placeholder} element in working against
 * the spring-context-3.1 or higher XSD; whereas, spring-context versions &lt;= 3.0 default to
 * {@code PropertyPlaceholderConfigurer} to ensure backward compatibility. See the spring-context
 * XSD documentation for complete details.
 *
 * <p>Any local properties (for example, those added via {@link #setProperties}, {@link #setLocations}
 * et al.) are added as a single {@link PropertySource}. Search precedence of local properties is
 * based on the value of the {@link #setLocalOverride localOverride} property, which is by
 * default {@code false} meaning that local properties are to be searched last, after all
 * environment property sources.
 *
 * <p>See {@link org.springframework.core.env.ConfigurableEnvironment} and related javadocs
 * for details on manipulating environment property sources.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 * @see org.springframework.core.env.ConfigurableEnvironment
 * @see org.springframework.beans.factory.config.PlaceholderConfigurerSupport
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
public class PropertySourcesPlaceholderConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

	/**
	 * {@value} is the name given to the {@link PropertySource} for the set of
	 * {@linkplain #mergeProperties() merged properties} supplied to this configurer.
	 */
	public static final String LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME = "localProperties";

	/**
	 * {@value} is the name given to the {@link PropertySource} that wraps the
	 * {@linkplain #setEnvironment environment} supplied to this configurer.
	 */
	public static final String ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME = "environmentProperties";


	private @Nullable MutablePropertySources propertySources;

	private @Nullable PropertySources appliedPropertySources;

	private @Nullable Environment environment;


	/**
	 * Customize the set of {@link PropertySources} to be used by this configurer.
	 * <p>Setting this property indicates that environment property sources and
	 * local properties should be ignored.
	 * @see #postProcessBeanFactory
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = new MutablePropertySources(propertySources);
	}

	/**
	 * {@inheritDoc}
	 * <p>{@code PropertySources} from the given {@link Environment} will be searched
	 * when replacing ${...} placeholders.
	 * @see #setPropertySources
	 * @see #postProcessBeanFactory
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}


	/**
	 * Processing occurs by replacing ${...} placeholders in bean definitions by resolving each
	 * against this configurer's set of {@link PropertySources}, which includes:
	 * <ul>
	 * <li>all {@linkplain org.springframework.core.env.ConfigurableEnvironment#getPropertySources
	 * environment property sources}, if an {@code Environment} {@linkplain #setEnvironment is present}
	 * <li>{@linkplain #mergeProperties merged local properties}, if {@linkplain #setLocation any}
	 * {@linkplain #setLocations have} {@linkplain #setProperties been}
	 * {@linkplain #setPropertiesArray specified}
	 * <li>any property sources set by calling {@link #setPropertySources}
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
				PropertySource<?> environmentPropertySource =
						(this.environment instanceof ConfigurableEnvironment configurableEnvironment ?
							new ConfigurableEnvironmentPropertySource(configurableEnvironment) :
							new FallbackEnvironmentPropertySource(this.environment));
				this.propertySources.addLast(environmentPropertySource);
			}
			try {
				PropertySource<?> localPropertySource =
						new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, mergeProperties());
				if (this.localOverride) {
					this.propertySources.addFirst(localPropertySource);
				}
				else {
					this.propertySources.addLast(localPropertySource);
				}
			}
			catch (IOException ex) {
				throw new BeanInitializationException("Could not load properties", ex);
			}
		}

		processProperties(beanFactory, createPropertyResolver(this.propertySources));
		this.appliedPropertySources = this.propertySources;
	}

	/**
	 * Create a {@link ConfigurablePropertyResolver} for the specified property sources.
	 * <p>The default implementation creates a {@link PropertySourcesPropertyResolver}.
	 * @param propertySources the property sources to use
	 * @since 6.0.12
	 */
	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources){
		return new PropertySourcesPropertyResolver(propertySources);
	}

	/**
	 * Visit each bean definition in the given bean factory and attempt to replace ${...} property
	 * placeholders with values from the given properties.
	 */
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
			ConfigurablePropertyResolver propertyResolver) throws BeansException {

		propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
		propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
		propertyResolver.setValueSeparator(this.valueSeparator);
		propertyResolver.setEscapeCharacter(this.escapeCharacter);

		StringValueResolver valueResolver = strVal -> {
			String resolved = (this.ignoreUnresolvablePlaceholders ?
					propertyResolver.resolvePlaceholders(strVal) :
					propertyResolver.resolveRequiredPlaceholders(strVal));
			if (this.trimValues) {
				resolved = resolved.trim();
			}
			return (resolved.equals(this.nullValue) ? null : resolved);
		};

		doProcessProperties(beanFactoryToProcess, valueResolver);
	}

	/**
	 * Implemented for compatibility with
	 * {@link org.springframework.beans.factory.config.PlaceholderConfigurerSupport}.
	 * @throws UnsupportedOperationException in this implementation
	 * @deprecated in favor of
	 * {@link #processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver)}
	 */
	@Override
	@Deprecated(since = "3.1")
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
		throw new UnsupportedOperationException(
				"Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
	}

	/**
	 * Return the property sources that were actually applied during
	 * {@link #postProcessBeanFactory(ConfigurableListableBeanFactory) post-processing}.
	 * @return the property sources that were applied
	 * @throws IllegalStateException if the property sources have not yet been applied
	 * @since 4.0
	 */
	public PropertySources getAppliedPropertySources() throws IllegalStateException {
		Assert.state(this.appliedPropertySources != null, "PropertySources have not yet been applied");
		return this.appliedPropertySources;
	}


	/**
	 * Custom {@link PropertySource} that delegates to the
	 * {@link ConfigurableEnvironment#getPropertySources() PropertySources} in a
	 * {@link ConfigurableEnvironment}.
	 * @since 6.2.7
	 */
	private static class ConfigurableEnvironmentPropertySource extends PropertySource<ConfigurableEnvironment> {

		ConfigurableEnvironmentPropertySource(ConfigurableEnvironment environment) {
			super(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, environment);
		}

		@Override
		public boolean containsProperty(String name) {
			for (PropertySource<?> propertySource : super.source.getPropertySources()) {
				if (propertySource.containsProperty(name)) {
					return true;
				}
			}
			return false;
		}

		@Override
		// Declare String as covariant return type, since a String is actually required.
		public @Nullable String getProperty(String name) {
			for (PropertySource<?> propertySource : super.source.getPropertySources()) {
				Object candidate = propertySource.getProperty(name);
				if (candidate != null) {
					return convertToString(candidate);
				}
			}
			return null;
		}

		/**
		 * Convert the supplied value to a {@link String} using the {@link ConversionService}
		 * from the {@link Environment}.
		 * <p>This is a modified version of
		 * {@link org.springframework.core.env.AbstractPropertyResolver#convertValueIfNecessary(Object, Class)}.
		 * @param value the value to convert
		 * @return the converted value, or the original value if no conversion is necessary
		 * @since 6.2.8
		 */
		private @Nullable String convertToString(Object value) {
			if (value instanceof String string) {
				return string;
			}
			return super.source.getConversionService().convert(value, String.class);
		}

		@Override
		public String toString() {
			return "ConfigurableEnvironmentPropertySource {propertySources=" + super.source.getPropertySources() + "}";
		}
	}


	/**
	 * Fallback {@link PropertySource} that delegates to a raw {@link Environment}.
	 * <p>Should never apply in a regular scenario, since the {@code Environment}
	 * in an {@code ApplicationContext} should always be a {@link ConfigurableEnvironment}.
	 * @since 6.2.7
	 */
	private static class FallbackEnvironmentPropertySource extends PropertySource<Environment> {

		FallbackEnvironmentPropertySource(Environment environment) {
			super(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, environment);
		}

		@Override
		public boolean containsProperty(String name) {
			return super.source.containsProperty(name);
		}

		@Override
		// Declare String as covariant return type, since a String is actually required.
		public @Nullable String getProperty(String name) {
			return super.source.getProperty(name);
		}

		@Override
		public String toString() {
			return "FallbackEnvironmentPropertySource {environment=" + super.source + "}";
		}
	}

}
