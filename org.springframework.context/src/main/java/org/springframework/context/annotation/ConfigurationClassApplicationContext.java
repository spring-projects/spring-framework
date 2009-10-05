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

package org.springframework.context.annotation;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * Standalone application context, accepting {@link Configuration}-annotated
 * class literals as input. Useful for test harnesses or any other scenario
 * where XML-based configuration is unnecessary or undesired.
 *
 * <p>In case of multiple Configuration classes, {@link Bean}
 * methods defined in later classes will override those defined in earlier
 * classes. This can be leveraged to deliberately override certain bean
 * definitions via an extra Configuration class.
 *
 * @author Chris Beams
 * @since 3.0
 * @see Configuration
 */
public class ConfigurationClassApplicationContext extends AbstractRefreshableApplicationContext {

	private final Set<Class<?>> configClasses = new LinkedHashSet<Class<?>>();

	/**
	 * Create a new {@link ConfigurationClassApplicationContext}, loading bean 
	 * definitions from the given {@literal configClasses} and automatically
	 * refreshing the context. <p>Note: if zero classes are specified, the
	 * context will <b>not</b> be refreshed automatically, assuming that
	 * the user will subsequently call {@link #addConfigurationClass(Class)}
	 * and then manually refresh.
	 * 
	 * @param configClasses zero or more {@link Configuration} classes
	 * @see #addConfigurationClass(Class)
	 * @see #refresh()
	 */
	public ConfigurationClassApplicationContext(Class<?>... configClasses) {
		if (configClasses.length == 0) {
			return;
		}
		
		for (Class<?> configClass : configClasses) {
			addConfigurationClass(configClass);
		}
		
		this.refresh();
	}
	
	/**
	 * Add a {@link Configuration} class to be processed. Allows for programmatically
	 * building a {@link ConfigurationClassApplicationContext}. Note that
	 * {@link ConfigurationClassApplicationContext#refresh()} must be called in
	 * order for the context to process the new class.  Calls to
	 * {@link #addConfigurationClass(Class)} are idempotent; adding the same
	 * Configuration class more than once has no additional effect.
	 * @param configClass new Configuration class to be processed.
	 * @see #ConfigurationClassApplicationContext(Class...)
	 * @see #refresh()
	 */
	public void addConfigurationClass(Class<?> configClass) {
		Assert.notNull(
				AnnotationUtils.findAnnotation(configClass, Configuration.class),
				"Class [" + configClass.getName() + "] is not annotated with @Configuration");
		this.configClasses.add(configClass);
	}

	/**
	 * Register a {@link BeanDefinition} for each {@link Configuration @Configuration}
	 * class specified. Enables the default set of annotation configuration post
	 * processors, such that {@literal @Autowired}, {@literal @Required}, and associated
	 * annotations can be used within Configuration classes.
	 * 
	 * <p>Configuration class bean definitions are registered with generated bean definition names.
	 * 
	 * @see AnnotationConfigUtils#registerAnnotationConfigProcessors(org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 * @see ConfigurationClassPostProcessor
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory)
			throws IOException, BeansException {
		
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
