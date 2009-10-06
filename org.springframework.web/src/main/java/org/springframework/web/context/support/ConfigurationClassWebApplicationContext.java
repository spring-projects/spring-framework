/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassApplicationContext;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;


/**
 * {@link org.springframework.web.context.WebApplicationContext} implementation
 * which takes its configuration from {@link Configuration @Configuration} classes.
 * This is essentially the equivalent of
 * {@link org.springframework.context.annotation.ConfigurationClassApplicationContext}
 * for a web environment.
 *
 * <p>To make use of this application context, the "contextClass" context-param for
 * ContextLoader and/or "contextClass" init-param for FrameworkServlet must be set to
 * the fully-qualified name of this class.
 *
 * <p>Unlike {@link XmlWebApplicationContext}, no default configuration class locations
 * are assumed. Rather, it is a requirement to set the "contextConfigLocation"
 * context-param for ContextLoader and/or "contextConfigLocation" init-param for
 * FrameworkServlet. If these params are not set, an IllegalArgumentException will be thrown.
 *
 * <p>Note: In case of multiple {@literal @Configuration} classes, later {@literal @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Chris Beams
 * @since 3.0
 * @see ConfigurationClassApplicationContext
 * @see ConfigurationClassApplicationContext.Delegate
 */
public class ConfigurationClassWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	private final ConfigurationClassApplicationContext.Delegate delegate =
			new ConfigurationClassApplicationContext.Delegate();

	/**
	 * Register a {@link BeanDefinition} for each {@link Configuration @Configuration}
	 * class specified by {@link #getConfigLocations()}. Enables the default set of
	 * annotation configuration post processors, such that {@literal @Autowired},
	 * {@literal @Required}, and associated annotations can be used within Configuration
	 * classes.
	 *
	 * <p>Configuration class bean definitions are registered with generated bean
	 * definition names unless the {@literal value} attribute is provided to the
	 * Configuration annotation.
	 *
	 * @throws IllegalArgumentException if configLocations array is null or empty
	 * @throws IOException if any one configLocation is not loadable as a class
	 * @throws IllegalArgumentException if any one loaded class is not annotated with {@literal @Configuration}
	 * @see #getConfigLocations()
	 * @see AnnotationConfigUtils#registerAnnotationConfigProcessors(org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 * @see ConfigurationClassPostProcessor
	 * @see DefaultBeanNameGenerator
	 * @see Configuration#value()
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws IOException, BeansException {

	 	Assert.notEmpty(getConfigLocations(),
	 			"No config locations were specified. Is the 'contextConfigLocations' " +
	 			"context-param and/or init-param set properly in web.xml?");

		for (String configLocation : getConfigLocations()) {
			try {
				Class<?> configClass = ClassUtils.getDefaultClassLoader().loadClass(configLocation);
				this.delegate.addConfigurationClass(configClass);
			} catch (ClassNotFoundException ex) {
				throw new IOException("Could not load @Configuration class [" + configLocation + "]", ex);
			}
		}

		this.delegate.loadBeanDefinitions(beanFactory);
	}

	/**
	 * Return the bean instance that matches the given object type.
	 *
	 * @param <T>
	 * @param requiredType type the bean must match; can be an interface or superclass.
	 * {@literal null} is disallowed.
	 * @return bean matching required type
	 * @throws NoSuchBeanDefinitionException if there is not exactly one matching bean
	 * found
	 * @see org.springframework.beans.factory.ListableBeanFactory#getBeansOfType(Class)
	 * @see org.springframework.beans.factory.BeanFactory#getBean(String, Class)
	 */
	public <T> T getBean(Class<T> requiredType) {
		return this.delegate.getBean(requiredType, this);
	}
}
