/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.mock.servlet.setup;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DelegatingMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

/**
 * A mock WebApplicationContext that accepts registrations of object instances.
 *
 * <p>As registered object instances are instantiated and initialized
 * externally, there is no wiring, bean initialization, lifecycle events, as
 * well as no pre-processing and post-processing hooks typically associated with
 * beans managed by an {@link ApplicationContext}. Just a simple lookup into a
 * {@link StaticListableBeanFactory}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
class StubWebApplicationContext implements WebApplicationContext {

	private final ServletContext servletContext;

	private final StubBeanFactory beanFactory = new StubBeanFactory();

	private final String id = ObjectUtils.identityToString(this);

	private final String displayName = ObjectUtils.identityToString(this);

	private final long startupDate = System.currentTimeMillis();

	private final Environment environment = new StandardEnvironment();

	private final MessageSource messageSource = new DelegatingMessageSource();

	private final ResourcePatternResolver resourcePatternResolver;


	/**
	 * Class constructor.
	 */
	public StubWebApplicationContext(ServletContext servletContext) {
		this.servletContext = servletContext;
		this.resourcePatternResolver = new ServletContextResourcePatternResolver(servletContext);
	}

	/**
	 * Returns an instance that can initialize {@link ApplicationContextAware} beans.
	 */
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return this.beanFactory;
	}

	public ServletContext getServletContext() {
		return this.servletContext;
	}

	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	public String getId() {
		return this.id;
	}

	public String getApplicationName() {
		return "";
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public long getStartupDate() {
		return this.startupDate;
	}

	public ApplicationContext getParent() {
		return null;
	}

	public Environment getEnvironment() {
		return this.environment ;
	}

	public void addBean(String name, Object bean) {
		this.beanFactory.addBean(name, bean);
	}

	public void addBeans(List<?> beans) {
		for (Object bean : beans) {
			String name = bean.getClass().getName() + "#" +  ObjectUtils.getIdentityHexString(bean);
			this.beanFactory.addBean(name, bean);
		}
	}

	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	public Object getBean(String name) throws BeansException {
		return this.beanFactory.getBean(name);
	}

	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return this.beanFactory.getBean(name, requiredType);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return this.beanFactory.getBean(requiredType);
	}

	public Object getBean(String name, Object... args) throws BeansException {
		return this.beanFactory.getBean(name, args);
	}

	public boolean containsBean(String name) {
		return this.beanFactory.containsBean(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return this.beanFactory.isSingleton(name);
	}

	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return this.beanFactory.isPrototype(name);
	}

	public boolean isTypeMatch(String name, Class<?> targetType) throws NoSuchBeanDefinitionException {
		return this.beanFactory.isTypeMatch(name, targetType);
	}

	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return this.beanFactory.getType(name);
	}

	public String[] getAliases(String name) {
		return this.beanFactory.getAliases(name);
	}

	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	public boolean containsBeanDefinition(String beanName) {
		return this.beanFactory.containsBeanDefinition(beanName);
	}

	public int getBeanDefinitionCount() {
		return this.beanFactory.getBeanDefinitionCount();
	}

	public String[] getBeanDefinitionNames() {
		return this.beanFactory.getBeanDefinitionNames();
	}

	public String[] getBeanNamesForType(Class<?> type) {
		return this.beanFactory.getBeanNamesForType(type);
	}

	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		return this.beanFactory.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
		return this.beanFactory.getBeansOfType(type);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		return this.beanFactory.getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		return this.beanFactory.getBeansWithAnnotation(annotationType);
	}

	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
		return this.beanFactory.findAnnotationOnBean(beanName, annotationType);
	}

	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	public BeanFactory getParentBeanFactory() {
		return null;
	}

	public boolean containsLocalBean(String name) {
		return this.beanFactory.containsBean(name);
	}

	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	public String getMessage(String code, Object args[], String defaultMessage, Locale locale) {
		return this.messageSource.getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object args[], Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, locale);
	}

	//---------------------------------------------------------------------
	// Implementation of ResourceLoader interface
	//---------------------------------------------------------------------

	public ClassLoader getClassLoader() {
		return null;
	}

	public Resource getResource(String location) {
		return this.resourcePatternResolver.getResource(location);
	}

	//---------------------------------------------------------------------
	// Other
	//---------------------------------------------------------------------

	public void publishEvent(ApplicationEvent event) {
	}

	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	/**
	 * An extension of StaticListableBeanFactory that implements
	 * AutowireCapableBeanFactory in order to allow bean initialization of
	 * {@link ApplicationContextAware} singletons.
	 */
	private class StubBeanFactory extends StaticListableBeanFactory implements AutowireCapableBeanFactory {

		public Object initializeBean(Object existingBean, String beanName) throws BeansException {
			if (existingBean instanceof ApplicationContextAware) {
				((ApplicationContextAware) existingBean).setApplicationContext(StubWebApplicationContext.this);
			}
			return existingBean;
		}

		public <T> T createBean(Class<T> beanClass) throws BeansException {
			throw new UnsupportedOperationException("Bean creation is not supported");
		}

		@SuppressWarnings("rawtypes")
		public Object createBean(Class beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
			throw new UnsupportedOperationException("Bean creation is not supported");
		}

		@SuppressWarnings("rawtypes")
		public Object autowire(Class beanClass, int autowireMode, boolean dependencyCheck) throws BeansException {
			return null;
		}

		public void autowireBean(Object existingBean) throws BeansException {
			throw new UnsupportedOperationException("Autowiring is not supported");
		}

		public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) throws BeansException {
			throw new UnsupportedOperationException("Autowiring is not supported");
		}

		public Object configureBean(Object existingBean, String beanName) throws BeansException {
			throw new UnsupportedOperationException("Configuring a bean is not supported");
		}

		public Object resolveDependency(DependencyDescriptor descriptor, String beanName) throws BeansException {
			throw new UnsupportedOperationException("Dependency resolution is not supported");
		}

		public Object resolveDependency(DependencyDescriptor descriptor, String beanName, Set<String> autowiredBeanNames,
				TypeConverter typeConverter) throws BeansException {
			throw new UnsupportedOperationException("Dependency resolution is not supported");
		}

		public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
			throw new UnsupportedOperationException("Bean property initialization is not supported");
		}

		public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
				throws BeansException {
			throw new UnsupportedOperationException("Post processing is not supported");
		}

		public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
				throws BeansException {
			throw new UnsupportedOperationException("Post processing is not supported");
		}
	}

}
