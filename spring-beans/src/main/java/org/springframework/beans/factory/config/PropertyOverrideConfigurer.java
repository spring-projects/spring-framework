/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.factory.config;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanInitializationException;

/**
 * Property resource configurer that overrides bean property values in an application
 * context definition. It <i>pushes</i> values from a properties file into bean definitions.
 *
 * <p>Configuration lines are expected to be of the following form:
 *
 * <pre class="code">beanName.property=value</pre>
 *
 * Example properties file:
 *
 * <pre class="code">dataSource.driverClassName=com.mysql.jdbc.Driver
 * dataSource.url=jdbc:mysql:mydb</pre>
 *
 * In contrast to PropertyPlaceholderConfigurer, the original definition can have default
 * values or no values at all for such bean properties. If an overriding properties file does
 * not have an entry for a certain bean property, the default context definition is used.
 *
 * <p>Note that the context definition <i>is not</i> aware of being overridden;
 * so this is not immediately obvious when looking at the XML definition file.
 * Furthermore, note that specified override values are always <i>literal</i> values;
 * they are not translated into bean references. This also applies when the original
 * value in the XML bean definition specifies a bean reference.
 *
 * <p>In case of multiple PropertyOverrideConfigurers that define different values for
 * the same bean property, the <i>last</i> one will win (due to the overriding mechanism).
 *
 * <p>Property values can be converted after reading them in, through overriding
 * the {@code convertPropertyValue} method. For example, encrypted values
 * can be detected and decrypted accordingly before processing them.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since 12.03.2003
 * @see #convertPropertyValue
 * @see PropertyPlaceholderConfigurer
 */
public class PropertyOverrideConfigurer extends PropertyResourceConfigurer {

	public static final String DEFAULT_BEAN_NAME_SEPARATOR = ".";


	private String beanNameSeparator = DEFAULT_BEAN_NAME_SEPARATOR;

	private boolean ignoreInvalidKeys = false;

	/**
	 * Contains names of beans that have overrides
	 */
	private final Set<String> beanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(16));


	/**
	 * Set the separator to expect between bean name and property path.
	 * Default is a dot (".").
	 */
	public void setBeanNameSeparator(String beanNameSeparator) {
		this.beanNameSeparator = beanNameSeparator;
	}

	/**
	 * Set whether to ignore invalid keys. Default is "false".
	 * <p>If you ignore invalid keys, keys that do not follow the 'beanName.property' format
	 * (or refer to invalid bean names or properties) will just be logged at debug level.
	 * This allows one to have arbitrary other keys in a properties file.
	 */
	public void setIgnoreInvalidKeys(boolean ignoreInvalidKeys) {
		this.ignoreInvalidKeys = ignoreInvalidKeys;
	}


	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props)
			throws BeansException {

		for (Enumeration<?> names = props.propertyNames(); names.hasMoreElements();) {
			String key = (String) names.nextElement();
			try {
				processKey(beanFactory, key, props.getProperty(key));
			}
			catch (BeansException ex) {
				String msg = "Could not process key '" + key + "' in PropertyOverrideConfigurer";
				if (!this.ignoreInvalidKeys) {
					throw new BeanInitializationException(msg, ex);
				}
				if (logger.isDebugEnabled()) {
					logger.debug(msg, ex);
				}
			}
		}
	}

	/**
	 * Process the given key as 'beanName.property' entry.
	 */
	protected void processKey(ConfigurableListableBeanFactory factory, String key, String value)
			throws BeansException {

		int separatorIndex = key.indexOf(this.beanNameSeparator);
		if (separatorIndex == -1) {
			throw new BeanInitializationException("Invalid key '" + key +
					"': expected 'beanName" + this.beanNameSeparator + "property'");
		}
		String beanName = key.substring(0, separatorIndex);
		String beanProperty = key.substring(separatorIndex+1);
		this.beanNames.add(beanName);
		applyPropertyValue(factory, beanName, beanProperty, value);
		if (logger.isDebugEnabled()) {
			logger.debug("Property '" + key + "' set to value [" + value + "]");
		}
	}

	/**
	 * Apply the given property value to the corresponding bean.
	 */
	protected void applyPropertyValue(
			ConfigurableListableBeanFactory factory, String beanName, String property, String value) {

		BeanDefinition bd = factory.getBeanDefinition(beanName);
		BeanDefinition bdToUse = bd;
		while (bd != null) {
			bdToUse = bd;
			bd = bd.getOriginatingBeanDefinition();
		}
		PropertyValue pv = new PropertyValue(property, value);
		pv.setOptional(this.ignoreInvalidKeys);
		bdToUse.getPropertyValues().addPropertyValue(pv);
	}


	/**
	 * Were there overrides for this bean?
	 * Only valid after processing has occurred at least once.
	 * @param beanName name of the bean to query status for
	 * @return whether there were property overrides for the named bean
	 */
	public boolean hasPropertyOverridesFor(String beanName) {
		return this.beanNames.contains(beanName);
	}

}
