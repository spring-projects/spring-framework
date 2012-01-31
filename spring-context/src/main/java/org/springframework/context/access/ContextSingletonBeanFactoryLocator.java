/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.context.access;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

/**
 * <p>Variant of {@link org.springframework.beans.factory.access.SingletonBeanFactoryLocator}
 * which creates its internal bean factory reference as an
 * {@link org.springframework.context.ApplicationContext} instead of
 * SingletonBeanFactoryLocator's simple BeanFactory. For almost all usage scenarios,
 * this will not make a difference, since within that ApplicationContext or BeanFactory
 * you are still free to define either BeanFactory or ApplicationContext instances.
 * The main reason one would need to use this class is if bean post-processing
 * (or other ApplicationContext specific features are needed in the bean reference
 * definition itself).
 *
 * <p><strong>Note:</strong> This class uses <strong>classpath*:beanRefContext.xml</strong>
 * as the default resource location for the bean factory reference definition files.
 * It is not possible nor legal to share definitions with SingletonBeanFactoryLocator
 * at the same time.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @see org.springframework.beans.factory.access.SingletonBeanFactoryLocator
 * @see org.springframework.context.access.DefaultLocatorFactory
 */
public class ContextSingletonBeanFactoryLocator extends SingletonBeanFactoryLocator {

	private static final String DEFAULT_RESOURCE_LOCATION = "classpath*:beanRefContext.xml";

	/** The keyed singleton instances */
	private static final Map<String, BeanFactoryLocator> instances = new HashMap<String, BeanFactoryLocator>();


	/**
	 * Returns an instance which uses the default "classpath*:beanRefContext.xml", as
	 * the name of the definition file(s). All resources returned by the current
	 * thread's context class loader's <code>getResources</code> method with this
	 * name will be combined to create a definition, which is just a BeanFactory.
	 * @return the corresponding BeanFactoryLocator instance
	 * @throws BeansException in case of factory loading failure
	 */
	public static BeanFactoryLocator getInstance() throws BeansException {
		return getInstance(null);
	}

	/**
	 * Returns an instance which uses the the specified selector, as the name of the
	 * definition file(s). In the case of a name with a Spring "classpath*:" prefix,
	 * or with no prefix, which is treated the same, the current thread's context class
	 * loader's <code>getResources</code> method will be called with this value to get
	 * all resources having that name. These resources will then be combined to form a
	 * definition. In the case where the name uses a Spring "classpath:" prefix, or
	 * a standard URL prefix, then only one resource file will be loaded as the
	 * definition.
	 * @param selector the location of the resource(s) which will be read and
	 * combined to form the definition for the BeanFactoryLocator instance.
	 * Any such files must form a valid ApplicationContext definition.
	 * @return the corresponding BeanFactoryLocator instance
	 * @throws BeansException in case of factory loading failure
	 */
	public static BeanFactoryLocator getInstance(String selector) throws BeansException {
		String resourceLocation = selector;
		if (resourceLocation == null) {
			resourceLocation = DEFAULT_RESOURCE_LOCATION;
		}

		// For backwards compatibility, we prepend "classpath*:" to the selector name if there
		// is no other prefix (i.e. "classpath*:", "classpath:", or some URL prefix).
		if (!ResourcePatternUtils.isUrl(resourceLocation)) {
			resourceLocation = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + resourceLocation;
		}

		synchronized (instances) {
			if (logger.isTraceEnabled()) {
				logger.trace("ContextSingletonBeanFactoryLocator.getInstance(): instances.hashCode=" +
						instances.hashCode() + ", instances=" + instances);
			}
			BeanFactoryLocator bfl = instances.get(resourceLocation);
			if (bfl == null) {
				bfl = new ContextSingletonBeanFactoryLocator(resourceLocation);
				instances.put(resourceLocation, bfl);
			}
			return bfl;
		}
	}


	/**
	 * Constructor which uses the the specified name as the resource name
	 * of the definition file(s).
	 * @param resourceLocation the Spring resource location to use
	 * (either a URL or a "classpath:" / "classpath*:" pseudo URL)
	 */
	protected ContextSingletonBeanFactoryLocator(String resourceLocation) {
		super(resourceLocation);
	}

	/**
	 * Overrides the default method to create definition object as an ApplicationContext
	 * instead of the default BeanFactory. This does not affect what can actually
	 * be loaded by that definition.
	 * <p>The default implementation simply builds a
	 * {@link org.springframework.context.support.ClassPathXmlApplicationContext}.
	 */
	@Override
	protected BeanFactory createDefinition(String resourceLocation, String factoryKey) {
		return new ClassPathXmlApplicationContext(new String[] {resourceLocation}, false);
	}

	/**
	 * Overrides the default method to refresh the ApplicationContext, invoking
	 * {@link ConfigurableApplicationContext#refresh ConfigurableApplicationContext.refresh()}.
	 */
	@Override
	protected void initializeDefinition(BeanFactory groupDef) {
		if (groupDef instanceof ConfigurableApplicationContext) {
			((ConfigurableApplicationContext) groupDef).refresh();
		}
	}

	/**
	 * Overrides the default method to operate on an ApplicationContext, invoking
	 * {@link ConfigurableApplicationContext#refresh ConfigurableApplicationContext.close()}.
	 */
	@Override
	protected void destroyDefinition(BeanFactory groupDef, String selector) {
		if (groupDef instanceof ConfigurableApplicationContext) {
			if (logger.isTraceEnabled()) {
				logger.trace("Context group with selector '" + selector +
						"' being released, as there are no more references to it");
			}
			((ConfigurableApplicationContext) groupDef).close();
		}
	}

}
