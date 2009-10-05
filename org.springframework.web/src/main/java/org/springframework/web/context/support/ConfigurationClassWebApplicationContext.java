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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;


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
 * @see org.springframework.context.annotation.ConfigurationClassApplicationContext
 */
public class ConfigurationClassWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	/**
	 * @throws IllegalArgumentException if configLocations array is null or empty
	 * @throws IOException if any one configLocation is not loadable as a class
	 * @throws IllegalArgumentException if any one loaded class is not annotated with {@literal @Configuration}
	 * @see #getConfigLocations()
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws IOException, BeansException {

	 	Assert.notEmpty(getConfigLocations(),
	 			"No config locations were specified. Is the 'contextConfigLocations' " +
	 			"context-param and/or init-param set properly in web.xml?");

		Set<Class<?>> configClasses = new LinkedHashSet<Class<?>>();

		for (String configLocation : getConfigLocations()) {
			try {
				Class<?> configClass = ClassUtils.getDefaultClassLoader().loadClass(configLocation);
				if (AnnotationUtils.findAnnotation(configClass, Configuration.class) == null) {
					throw new IllegalArgumentException("Class [" + configClass.getName() + "] is not annotated with @Configuration");
				}
				configClasses.add(configClass);
			} catch (ClassNotFoundException ex) {
				throw new IOException("Could not load @Configuration class [" + configLocation + "]", ex);
			}
		}

		// @Autowired and friends must be enabled by default when processing @Configuration classes
		AnnotationConfigUtils.registerAnnotationConfigProcessors(beanFactory);

		for (Class<?> configClass : configClasses) {
			AbstractBeanDefinition def = BeanDefinitionBuilder.rootBeanDefinition(configClass).getBeanDefinition();

			String name = AnnotationUtils.findAnnotation(configClass, Configuration.class).value();
			if (!StringUtils.hasLength(name)) {
				name = new DefaultBeanNameGenerator().generateBeanName(def, beanFactory);
			}

			beanFactory.registerBeanDefinition(name, def);
		}

		new ConfigurationClassPostProcessor().postProcessBeanFactory(beanFactory);
	}

	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> requiredType) {
		Assert.notNull(requiredType, "requiredType may not be null");

		Map<String, ?> beansOfType = this.getBeansOfType(requiredType);

		switch (beansOfType.size()) {
			case 0:
				throw new NoSuchBeanDefinitionException(requiredType);
			case 1:
				return (T) beansOfType.values().iterator().next();
			default:
				throw new NoSuchBeanDefinitionException(requiredType,
						beansOfType.size() + " matching bean definitions found " +
						"(" + StringUtils.collectionToCommaDelimitedString(beansOfType.keySet()) + "). " +
						"Consider qualifying with getBean(Class<T> beanType, String beanName) or " +
						"declaring one bean definition as @Primary");
		}
	}
}
