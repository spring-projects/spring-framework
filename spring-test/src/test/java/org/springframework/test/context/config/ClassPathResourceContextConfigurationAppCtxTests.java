/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ResourceUtils;

/**
 * Extension of {@link CoreContextConfigurationAppCtxTests}, which verifies that
 * we can specify an explicit, <em>classpath</em> location for our application
 * context.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see CoreContextConfigurationAppCtxTests
 * @see #CLASSPATH_CONTEXT_RESOURCE_PATH
 * @see AbsolutePathContextConfigurationAppCtxTests
 * @see RelativePathContextConfigurationAppCtxTests
 */
@ContextConfiguration(locations = { ClassPathResourceContextConfigurationAppCtxTests.CLASSPATH_CONTEXT_RESOURCE_PATH }, inheritLocations = false)
class ClassPathResourceContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {

	/**
	 * Classpath-based resource path for the application context configuration
	 * for {@link CoreContextConfigurationAppCtxTests}: {@value}
	 *
	 * @see CoreContextConfigurationAppCtxTests#DEFAULT_CONTEXT_RESOURCE_PATH
	 * @see ResourceUtils#CLASSPATH_URL_PREFIX
	 */
	public static final String CLASSPATH_CONTEXT_RESOURCE_PATH = ResourceUtils.CLASSPATH_URL_PREFIX +
			CoreContextConfigurationAppCtxTests.DEFAULT_CONTEXT_RESOURCE_PATH;

	/* all tests are in the parent class. */

}
