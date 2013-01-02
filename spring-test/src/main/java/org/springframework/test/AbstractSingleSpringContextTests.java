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

package org.springframework.test;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * <p>
 * Abstract JUnit 3.8 test class that holds and exposes a single Spring
 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
 * </p>
 * <p>
 * This class will cache contexts based on a <i>context key</i>: normally the
 * config locations String array describing the Spring resource descriptors
 * making up the context. Unless the {@link #setDirty()} method is called by a
 * test, the context will not be reloaded, even across different subclasses of
 * this test. This is particularly beneficial if your context is slow to
 * construct, for example if you are using Hibernate and the time taken to load
 * the mappings is an issue.
 * </p>
 * <p>
 * For such standard usage, simply override the {@link #getConfigLocations()}
 * method and provide the desired config files. For alternative configuration
 * options, see {@link #getConfigPath()} and {@link #getConfigPaths()}.
 * </p>
 * <p>
 * If you don't want to load a standard context from an array of config
 * locations, you can override the {@link #contextKey()} method. In conjunction
 * with this you typically need to override the {@link #loadContext(Object)}
 * method, which by default loads the locations specified in the
 * {@link #getConfigLocations()} method.
 * </p>
 * <p>
 * <b>WARNING:</b> When doing integration tests from within Eclipse, only use
 * classpath resource URLs. Else, you may see misleading failures when changing
 * context locations.
 * </p>
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see #getConfigLocations()
 * @see #contextKey()
 * @see #loadContext(Object)
 * @see #getApplicationContext()
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests})
 */
@Deprecated
public abstract class AbstractSingleSpringContextTests extends AbstractSpringContextTests {

	/** Application context this test will run against */
	protected ConfigurableApplicationContext applicationContext;

	private int loadCount = 0;


	/**
	 * Default constructor for AbstractSingleSpringContextTests.
	 */
	public AbstractSingleSpringContextTests() {
	}

	/**
	 * Constructor for AbstractSingleSpringContextTests with a JUnit name.
	 * @param name the name of this text fixture
	 */
	public AbstractSingleSpringContextTests(String name) {
		super(name);
	}

	/**
	 * This implementation is final. Override {@code onSetUp} for custom behavior.
	 * @see #onSetUp()
	 */
	protected final void setUp() throws Exception {
		// lazy load, in case getApplicationContext() has not yet been called.
		if (this.applicationContext == null) {
			this.applicationContext = getContext(contextKey());
		}
		prepareTestInstance();
		onSetUp();
	}

	/**
	 * Prepare this test instance, for example populating its fields.
	 * The context has already been loaded at the time of this callback.
	 * <p>The default implementation does nothing.
	 * @throws Exception in case of preparation failure
	 */
	protected void prepareTestInstance() throws Exception {
	}

	/**
	 * Subclasses can override this method in place of the {@code setUp()}
	 * method, which is final in this class.
	 * <p>The default implementation does nothing.
	 * @throws Exception simply let any exception propagate
	 */
	protected void onSetUp() throws Exception {
	}

	/**
	 * Called to say that the "applicationContext" instance variable is dirty
	 * and should be reloaded. We need to do this if a test has modified the
	 * context (for example, by replacing a bean definition).
	 */
	protected void setDirty() {
		setDirty(contextKey());
	}

	/**
	 * This implementation is final. Override {@code onTearDown} for
	 * custom behavior.
	 * @see #onTearDown()
	 */
	protected final void tearDown() throws Exception {
		onTearDown();
	}

	/**
	 * Subclasses can override this to add custom behavior on teardown.
	 * @throws Exception simply let any exception propagate
	 */
	protected void onTearDown() throws Exception {
	}

	/**
	 * Return a key for this context. Default is the config location array as
	 * determined by {@link #getConfigLocations()}.
	 * <p>If you override this method, you will typically have to override
	 * {@link #loadContext(Object)} as well, being able to handle the key type
	 * that this method returns.
	 * @return the context key
	 * @see #getConfigLocations()
	 */
	protected Object contextKey() {
		return getConfigLocations();
	}

	/**
	 * This implementation assumes a key of type String array and loads a
	 * context from the given locations.
	 * <p>If you override {@link #contextKey()}, you will typically have to
	 * override this method as well, being able to handle the key type that
	 * {@code contextKey()} returns.
	 * @see #getConfigLocations()
	 */
	protected ConfigurableApplicationContext loadContext(Object key) throws Exception {
		return loadContextLocations((String[]) key);
	}

	/**
	 * Load a Spring ApplicationContext from the given config locations.
	 * <p>The default implementation creates a standard
	 * {@link #createApplicationContext GenericApplicationContext}, allowing
	 * for customizing the internal bean factory through
	 * {@link #customizeBeanFactory}.
	 * @param locations the config locations (as Spring resource locations,
	 * e.g. full classpath locations or any kind of URL)
	 * @return the corresponding ApplicationContext instance (potentially cached)
	 * @throws Exception if context loading failed
	 * @see #createApplicationContext(String[])
	 */
	protected ConfigurableApplicationContext loadContextLocations(String[] locations) throws Exception {
		++this.loadCount;
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Loading context for locations: " + StringUtils.arrayToCommaDelimitedString(locations));
		}
		return createApplicationContext(locations);
	}

	/**
	 * Create a Spring {@link ConfigurableApplicationContext} for use by this test.
	 * <p>The default implementation creates a standard {@link GenericApplicationContext}
	 * instance, calls the {@link #prepareApplicationContext} prepareApplicationContext}
	 * method and the {@link #customizeBeanFactory customizeBeanFactory} method to allow
	 * for customizing the context and its DefaultListableBeanFactory, populates the
	 * context from the specified config {@code locations} through the configured
	 * {@link #createBeanDefinitionReader(GenericApplicationContext) BeanDefinitionReader},
	 * and finally {@link ConfigurableApplicationContext#refresh() refreshes} the context.
	 * @param locations the config locations (as Spring resource locations,
	 * e.g. full classpath locations or any kind of URL)
	 * @return the GenericApplicationContext instance
	 * @see #loadContextLocations(String[])
	 * @see #customizeBeanFactory(DefaultListableBeanFactory)
	 * @see #createBeanDefinitionReader(GenericApplicationContext)
	 */
	protected ConfigurableApplicationContext createApplicationContext(String[] locations) {
		GenericApplicationContext context = new GenericApplicationContext();
		prepareApplicationContext(context);
		customizeBeanFactory(context.getDefaultListableBeanFactory());
		createBeanDefinitionReader(context).loadBeanDefinitions(locations);
		context.refresh();
		return context;
	}

	/**
	 * Prepare the GenericApplicationContext used by this test.
	 * Called before bean definitions are read.
	 * <p>The default implementation is empty. Can be overridden in subclasses to
	 * customize GenericApplicationContext's standard settings.
	 * @param context the context for which the BeanDefinitionReader should be created
	 * @see #createApplicationContext
	 * @see org.springframework.context.support.GenericApplicationContext#setResourceLoader
	 * @see org.springframework.context.support.GenericApplicationContext#setId
	 */
	protected void prepareApplicationContext(GenericApplicationContext context) {
	}

	/**
	 * Customize the internal bean factory of the ApplicationContext used by
	 * this test. Called before bean definitions are read.
	 * <p>The default implementation is empty. Can be overridden in subclasses to
	 * customize DefaultListableBeanFactory's standard settings.
	 * @param beanFactory the newly created bean factory for this context
	 * @see #loadContextLocations
	 * @see #createApplicationContext
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowBeanDefinitionOverriding
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowEagerClassLoading
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowCircularReferences
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#setAllowRawInjectionDespiteWrapping
	 */
	protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
	}

	/**
	 * Factory method for creating new {@link BeanDefinitionReader}s for
	 * loading bean definitions into the supplied
	 * {@link GenericApplicationContext context}.
	 * <p>The default implementation creates a new {@link XmlBeanDefinitionReader}.
	 * Can be overridden in subclasses to provide a different
	 * BeanDefinitionReader implementation.
	 * @param context the context for which the BeanDefinitionReader should be created
	 * @return a BeanDefinitionReader for the supplied context
	 * @see #createApplicationContext(String[])
	 * @see BeanDefinitionReader
	 * @see XmlBeanDefinitionReader
	 */
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		return new XmlBeanDefinitionReader(context);
	}

	/**
	 * Subclasses can override this method to return the locations of their
	 * config files, unless they override {@link #contextKey()} and
	 * {@link #loadContext(Object)} instead.
	 * <p>A plain path will be treated as class path location, e.g.:
	 * "org/springframework/whatever/foo.xml". Note however that you may prefix
	 * path locations with standard Spring resource prefixes. Therefore, a
	 * config location path prefixed with "classpath:" with behave the same as a
	 * plain path, but a config location such as
	 * "file:/some/path/path/location/appContext.xml" will be treated as a
	 * filesystem location.
	 * <p>The default implementation builds config locations for the config paths
	 * specified through {@link #getConfigPaths()}.
	 * @return an array of config locations
	 * @see #getConfigPaths()
	 * @see org.springframework.core.io.ResourceLoader#getResource(String)
	 */
	protected String[] getConfigLocations() {
		String[] paths = getConfigPaths();
		String[] locations = new String[paths.length];
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (path.startsWith("/")) {
				locations[i] = ResourceUtils.CLASSPATH_URL_PREFIX + path;
			}
			else {
				locations[i] = ResourceUtils.CLASSPATH_URL_PREFIX +
						StringUtils.cleanPath(ClassUtils.classPackageAsResourcePath(getClass()) + "/" + path);
			}
		}
		return locations;
	}

	/**
	 * Subclasses can override this method to return paths to their config
	 * files, relative to the concrete test class.
	 * <p>A plain path, e.g. "context.xml", will be loaded as classpath resource
	 * from the same package that the concrete test class is defined in. A path
	 * starting with a slash is treated as fully qualified class path location,
	 * e.g.: "/org/springframework/whatever/foo.xml".
	 * <p>The default implementation builds an array for the config path specified
	 * through {@link #getConfigPath()}.
	 * @return an array of config locations
	 * @see #getConfigPath()
	 * @see java.lang.Class#getResource(String)
	 */
	protected String[] getConfigPaths() {
		String path = getConfigPath();
		return (path != null ? new String[] { path } : new String[0]);
	}

	/**
	 * Subclasses can override this method to return a single path to a config
	 * file, relative to the concrete test class.
	 * <p>A plain path, e.g. "context.xml", will be loaded as classpath resource
	 * from the same package that the concrete test class is defined in. A path
	 * starting with a slash is treated as fully qualified class path location,
	 * e.g.: "/org/springframework/whatever/foo.xml".
	 * <p>The default implementation simply returns {@code null}.
	 * @return an array of config locations
	 * @see #getConfigPath()
	 * @see Class#getResource(String)
	 */
	protected String getConfigPath() {
		return null;
	}

	/**
	 * Return the ApplicationContext that this base class manages; may be
	 * {@code null}.
	 */
	public final ConfigurableApplicationContext getApplicationContext() {
		// lazy load, in case setUp() has not yet been called.
		if (this.applicationContext == null) {
			try {
				this.applicationContext = getContext(contextKey());
			}
			catch (Exception e) {
				// log and continue...
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("Caught exception while retrieving the ApplicationContext for test [" +
							getClass().getName() + "." + getName() + "].", e);
				}
			}
		}

		return this.applicationContext;
	}

	/**
	 * Return the current number of context load attempts.
	 */
	public final int getLoadCount() {
		return this.loadCount;
	}

}
