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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ScopeMetadataResolver;

/**
 * {@link org.springframework.web.context.WebApplicationContext} implementation
 * which accepts annotated classes as input - in particular
 * {@link org.springframework.context.annotation.Configuration @Configuration}-annotated
 * classes, but also plain {@link org.springframework.stereotype.Component @Components}
 * and JSR-330 compliant classes using {@literal javax.inject} annotations. Allows for
 * registering classes one by one (specifying class names as config location) as well
 * as for classpath scanning (specifying base packages as config location).
 *
 * <p>This is essentially the equivalent of
 * {@link org.springframework.context.annotation.AnnotationConfigApplicationContext}
 * for a web environment.
 *
 * <p>To make use of this application context, the "contextClass" context-param for
 * ContextLoader and/or "contextClass" init-param for FrameworkServlet must be set to
 * the fully-qualified name of this class.
 *
 * <p>Unlike {@link XmlWebApplicationContext}, no default configuration class locations
 * are assumed. Rather, it is a requirement to set the "contextConfigLocation"
 * context-param for ContextLoader and/or "contextConfigLocation" init-param for
 * FrameworkServlet.  The param-value may contain both fully-qualified
 * class names and base packages to scan for components.
 *
 * <p>Note: In case of multiple {@literal @Configuration} classes, later {@literal @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.springframework.context.annotation.AnnotationConfigApplicationContext
 */
public class AnnotationConfigWebApplicationContext extends AbstractRefreshableWebApplicationContext {

	/**
	 * Register a {@link BeanDefinition} for each class specified by {@link #getConfigLocations()},
	 * or scan each specified package for annotated classes. Enables the default set of
	 * annotation configuration post processors, such that {@literal @Autowired},
	 * {@literal @Required}, and associated annotations can be used.
	 * <p>Configuration class bean definitions are registered with generated bean definition
	 * names unless the {@literal value} attribute is provided to the stereotype annotation.
	 * @see #getConfigLocations()
	 * @see AnnotatedBeanDefinitionReader
	 * @see ClassPathBeanDefinitionScanner
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
		AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(beanFactory);
		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanFactory);
		BeanNameGenerator beanNameGenerator = getBeanNameGenerator();
		ScopeMetadataResolver scopeMetadataResolver = getScopeMetadataResolver();
		if (beanNameGenerator != null) {
			reader.setBeanNameGenerator(beanNameGenerator);
			scanner.setBeanNameGenerator(beanNameGenerator);
		}
		if (scopeMetadataResolver != null) {
			reader.setScopeMetadataResolver(scopeMetadataResolver);
			scanner.setScopeMetadataResolver(scopeMetadataResolver);
		}

		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			for (String configLocation : configLocations) {
				try {
					Class<?> clazz = getClassLoader().loadClass(configLocation);
					if (logger.isInfoEnabled()) {
						logger.info("Successfully resolved class for [" + configLocation + "]");
					}
					reader.register(clazz);
				}
				catch (ClassNotFoundException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not load class for config location [" + configLocation +
								"] - trying package scan. " + ex);
					}
					int count = scanner.scan(configLocation);
					if (logger.isInfoEnabled()) {
						if (count == 0) {
							logger.info("No annotated classes found for specified class/package [" + configLocation + "]");
						}
						else {
							logger.info("Found " + count + " annotated classes in package [" + configLocation + "]");
						}
					}
				}
			}
		}
	}

	/**
	 * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 * <p>Default is {@link org.springframework.context.annotation.AnnotationBeanNameGenerator}.
	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
	 */
	protected BeanNameGenerator getBeanNameGenerator() {
		return null;
	}

	/**
	 * Provide a custom {@link ScopeMetadataResolver} for use with {@link AnnotatedBeanDefinitionReader}
	 * and/or {@link ClassPathBeanDefinitionScanner}, if any.
	 * <p>Default is {@link org.springframework.context.annotation.AnnotationScopeMetadataResolver}.
	 * @see AnnotatedBeanDefinitionReader#setScopeMetadataResolver
	 * @see ClassPathBeanDefinitionScanner#setScopeMetadataResolver
	 */
	protected ScopeMetadataResolver getScopeMetadataResolver() {
		return null;
	}

}
