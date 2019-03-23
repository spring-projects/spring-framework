/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * This class is only used within tests in the spring-orm module.
 *
 * <p>Abstract JUnit 3.8 test class that holds and exposes a single Spring
 * {@link org.springframework.context.ApplicationContext ApplicationContext}.
 *
 * <p>This class will cache contexts based on a <i>context key</i>: normally the
 * config locations String array describing the Spring resource descriptors
 * making up the context. Unless the {@link #setDirty()} method is called by a
 * test, the context will not be reloaded, even across different subclasses of
 * this test. This is particularly beneficial if your context is slow to
 * construct, for example if you are using Hibernate and the time taken to load
 * the mappings is an issue.
 *
 * <p>For such standard usage, simply override the {@link #getConfigLocations()}
 * method and provide the desired config files. For alternative configuration
 * options, see {@link #getConfigPaths()}.
 *
 * <p><b>WARNING:</b> When doing integration tests from within Eclipse, only use
 * classpath resource URLs. Else, you may see misleading failures when changing
 * context locations.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @see #getConfigLocations()
 * @see #getApplicationContext()
 * @deprecated as of Spring 3.0, in favor of using the listener-based test context framework
 * ({@link org.springframework.test.context.junit38.AbstractJUnit38SpringContextTests})
 */
@Deprecated
abstract class AbstractSingleSpringContextTests extends AbstractSpringContextTests {

	private static final String SLASH = "/";

	/** Application context this test will run against */
	protected ConfigurableApplicationContext applicationContext;

	/**
	 * This implementation is final. Override {@code onSetUp} for custom behavior.
	 * @see #onSetUp()
	 */
	@Override
	protected final void setUp() throws Exception {
		// lazy load, in case getApplicationContext() has not yet been called.
		if (this.applicationContext == null) {
			this.applicationContext = getContext(getConfigLocations());
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
	 * This implementation is final. Override {@code onTearDown} for
	 * custom behavior.
	 * @see #onTearDown()
	 */
	@Override
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
	@Override
	protected ConfigurableApplicationContext loadContext(String... locations) throws Exception {
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
	 * @see #loadContext(String...)
	 * @see #customizeBeanFactory(DefaultListableBeanFactory)
	 * @see #createBeanDefinitionReader(GenericApplicationContext)
	 */
	private ConfigurableApplicationContext createApplicationContext(String... locations) {
		GenericApplicationContext context = new GenericApplicationContext();
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(locations);
		AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
		context.refresh();
		return context;
	}

	/**
	 * Subclasses can override this method to return the locations of their
	 * config files.
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
	protected final String[] getConfigLocations() {
		String[] paths = getConfigPaths();
		String[] convertedPaths = new String[paths.length];
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (path.startsWith(SLASH)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + path;
			}
			else if (!ResourcePatternUtils.isUrl(path)) {
				convertedPaths[i] = ResourceUtils.CLASSPATH_URL_PREFIX + SLASH
						+ StringUtils.cleanPath(ClassUtils.classPackageAsResourcePath(getClass()) + SLASH + path);
			}
			else {
				convertedPaths[i] = StringUtils.cleanPath(path);
			}
		}
		return convertedPaths;
	}

	/**
	 * Subclasses must override this method to return paths to their config
	 * files, relative to the concrete test class.
	 * <p>A plain path, e.g. "context.xml", will be loaded as classpath resource
	 * from the same package that the concrete test class is defined in. A path
	 * starting with a slash is treated as fully qualified class path location,
	 * e.g.: "/org/springframework/whatever/foo.xml".
	 * <p>The default implementation returns an empty array.
	 * @return an array of config locations
	 * @see java.lang.Class#getResource(String)
	 */
	protected abstract String[] getConfigPaths();

	/**
	 * Return the ApplicationContext that this base class manages; may be
	 * {@code null}.
	 */
	protected final ConfigurableApplicationContext getApplicationContext() {
		// lazy load, in case setUp() has not yet been called.
		if (this.applicationContext == null) {
			try {
				this.applicationContext = getContext(getConfigLocations());
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

}
