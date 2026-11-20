/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import java.lang.annotation.Inherited;

import org.springframework.test.context.ContextConfiguration;

/**
 * Extension of {@link CoreContextConfigurationAppCtxTests} which verifies that
 * the configuration of an application context and dependency injection of a
 * test instance function as expected within a class hierarchy, since
 * {@link ContextConfiguration configuration} is {@link Inherited inherited}.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see CoreContextConfigurationAppCtxTests
 */
class InheritedConfigContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {
	/* all tests are in the parent class. */
}
