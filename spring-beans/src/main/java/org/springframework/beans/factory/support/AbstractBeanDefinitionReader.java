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

package org.springframework.beans.factory.support;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

/**
 * Abstract base class for bean definition readers which implement
 * the {@link BeanDefinitionReader} interface.
 *
 * <p>Provides common properties like the bean factory to work on
 * and the class loader to use for loading bean classes.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 11.12.2003
 * @see BeanDefinitionReaderUtils
 */
public abstract class AbstractBeanDefinitionReader implements EnvironmentCapable, BeanDefinitionReader {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private final BeanDefinitionRegistry registry;

	private ResourceLoader resourceLoader;

	private ClassLoader beanClassLoader;

	private Environment environment = new StandardEnvironment();

	private BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();


	/**
	 * Create a new AbstractBeanDefinitionReader for the given bean factory.
	 * <p>If the passed-in bean factory does not only implement the BeanDefinitionRegistry
	 * interface but also the ResourceLoader interface, it will be used as default
	 * ResourceLoader as well. This will usually be the case for
	 * {@link org.springframework.context.ApplicationContext} implementations.
	 * <p>If given a plain BeanDefinitionRegistry, the default ResourceLoader will be a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
	 * <p>If the the passed-in bean factory also implements {@link EnvironmentCapable} its
	 * environment will be used by this reader.  Otherwise, the reader will initialize and
	 * use a {@link StandardEnvironment}. All ApplicationContext implementations are
	 * EnvironmentCapable, while normal BeanFactory implementations are not.
	 * @param registry the BeanFactory to load bean definitions into,
	 * in the form of a BeanDefinitionRegistry
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		// Determine ResourceLoader to use.
		if (this.registry instanceof ResourceLoader) {
			this.resourceLoader = (ResourceLoader) this.registry;
		}
		else {
			this.resourceLoader = new PathMatchingResourcePatternResolver();
		}

		// Inherit Environment if possible
		if (this.registry instanceof EnvironmentCapable) {
			this.environment = ((EnvironmentCapable)this.registry).getEnvironment();
		}
		else {
			this.environment = new StandardEnvironment();
		}
	}


	public final BeanDefinitionRegistry getBeanFactory() {
		return this.registry;
	}

	@Override
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the ResourceLoader to use for resource locations.
	 * If specifying a ResourcePatternResolver, the bean definition reader
	 * will be capable of resolving resource patterns to Resource arrays.
	 * <p>Default is PathMatchingResourcePatternResolver, also capable of
	 * resource pattern resolving through the ResourcePatternResolver interface.
	 * <p>Setting this to {@code null} suggests that absolute resource loading
	 * is not available for this bean definition reader.
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Set the ClassLoader to use for bean classes.
	 * <p>Default is {@code null}, which suggests to not load bean classes
	 * eagerly but rather to just register bean definitions with class names,
	 * with the corresponding Classes to be resolved later (or never).
	 * @see Thread#getContextClassLoader()
	 */
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Set the Environment to use when reading bean definitions. Most often used
	 * for evaluating profile information to determine which bean definitions
	 * should be read and which should be omitted.
	 */
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * Set the BeanNameGenerator to use for anonymous beans
	 * (without explicit bean name specified).
	 * <p>Default is a {@link DefaultBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new DefaultBeanNameGenerator());
	}

	@Override
	public BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}


	@Override
	public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
		Assert.notNull(resources, "Resource array must not be null");
		int counter = 0;
		for (Resource resource : resources) {
			counter += loadBeanDefinitions(resource);
		}
		return counter;
	}

	@Override
	public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(location, null);
	}

	/**
	 * Load bean definitions from the specified resource location.
	 * <p>The location can also be a location pattern, provided that the
	 * ResourceLoader of this bean definition reader is a ResourcePatternResolver.
	 * @param location the resource location, to be loaded with the ResourceLoader
	 * (or ResourcePatternResolver) of this bean definition reader
	 * @param actualResources a Set to be filled with the actual Resource objects
	 * that have been resolved during the loading process. May be {@code null}
	 * to indicate that the caller is not interested in those Resource objects.
	 * @return the number of bean definitions found
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 * @see #getResourceLoader()
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
	 */
	public int loadBeanDefinitions(String location, Set<Resource> actualResources) throws BeanDefinitionStoreException {
		ResourceLoader resourceLoader = getResourceLoader();
		if (resourceLoader == null) {
			throw new BeanDefinitionStoreException(
					"Cannot import bean definitions from location [" + location + "]: no ResourceLoader available");
		}

		if (resourceLoader instanceof ResourcePatternResolver) {
			// Resource pattern matching available.
			try {
				Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);
				int loadCount = loadBeanDefinitions(resources);
				if (actualResources != null) {
					for (Resource resource : resources) {
						actualResources.add(resource);
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + loadCount + " bean definitions from location pattern [" + location + "]");
				}
				return loadCount;
			}
			catch (IOException ex) {
				throw new BeanDefinitionStoreException(
						"Could not resolve bean definition resource pattern [" + location + "]", ex);
			}
		}
		else {
			// Can only load single resources by absolute URL.
			Resource resource = resourceLoader.getResource(location);
			int loadCount = loadBeanDefinitions(resource);
			if (actualResources != null) {
				actualResources.add(resource);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + loadCount + " bean definitions from location [" + location + "]");
			}
			return loadCount;
		}
	}

	@Override
	public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
		Assert.notNull(locations, "Location array must not be null");
		int counter = 0;
		for (String location : locations) {
			counter += loadBeanDefinitions(location);
		}
		return counter;
	}

}
