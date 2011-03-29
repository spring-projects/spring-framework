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

package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ObjectUtils;

/**
 * TODO Document AnnotationConfigContextLoader.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class AnnotationConfigContextLoader extends AbstractContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);


	/**
	 * TODO Document loadContext().
	 * 
	 * @see org.springframework.test.context.ContextLoader#loadContext(java.lang.String[])
	 */
	public ApplicationContext loadContext(String... locations) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Creating an AnnotationConfigApplicationContext for "
					+ ObjectUtils.nullSafeToString(locations));
		}

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		prepareContext(context);
		customizeBeanFactory(context.getDefaultListableBeanFactory());

		List<Class<?>> configClasses = new ArrayList<Class<?>>();
		for (String location : locations) {
			final Class<?> clazz = getClass().getClassLoader().loadClass(location);
			configClasses.add(clazz);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Loading AnnotationConfigApplicationContext from config classes: " + configClasses);
		}

		for (Class<?> configClass : configClasses) {
			context.register(configClass);
		}

		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		customizeContext(context);
		context.refresh();
		context.registerShutdownHook();

		return context;
	}

	/**
	 * Prepare the {@link GenericApplicationContext} created by this
	 * ContextLoader. Called <i>before</> bean definitions are read.
	 * <p>
	 * The default implementation is empty. Can be overridden in subclasses to
	 * customize GenericApplicationContext's standard settings.
	 * 
	 * @param context the context for which the BeanDefinitionReader should be
	 * created
	 * @see #loadContext
	 * @see org.springframework.context.support.GenericApplicationContext#setResourceLoader
	 * @see org.springframework.context.support.GenericApplicationContext#setId
	 */
	protected void prepareContext(GenericApplicationContext context) {
	}

	/**
	 * Customize the internal bean factory of the ApplicationContext created by
	 * this ContextLoader.
	 * <p>
	 * The default implementation is empty but can be overridden in subclasses
	 * to customize DefaultListableBeanFactory's standard settings.
	 * 
	 * @param beanFactory the bean factory created by this ContextLoader
	 * @see #loadContext
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding(boolean)
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading(boolean)
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences(boolean)
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping(boolean)
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
	}

	/**
	 * Customize the {@link GenericApplicationContext} created by this
	 * ContextLoader <i>after</i> bean definitions have been loaded into the
	 * context but before the context is refreshed.
	 * <p>
	 * The default implementation is empty but can be overridden in subclasses
	 * to customize the application context.
	 * 
	 * @param context the newly created application context
	 * @see #loadContext(String...)
	 */
	protected void customizeContext(GenericApplicationContext context) {
	}

	/**
	 * TODO Document overridden generateDefaultLocations().
	 * 
	 * @see org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations(java.lang.Class)
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		// TODO Implement generateDefaultLocations().
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * TODO Document modifyLocations().
	 * 
	 * @see org.springframework.test.context.support.AbstractContextLoader#modifyLocations(java.lang.Class,
	 * java.lang.String[])
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		// TODO Implement modifyLocations() (?).
		return locations;
	}

	/**
	 * TODO Document getResourceSuffix().
	 * 
	 * @see org.springframework.test.context.support.AbstractContextLoader#getResourceSuffix()
	 */
	@Override
	protected String getResourceSuffix() {
		return "Config";
	}

}
