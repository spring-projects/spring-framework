/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.springframework.test.context.ContextConfiguration;

/**
 * Extension of {@link CoreContextConfigurationAppCtxTests}, which verifies that
 * we can specify an explicit, <em>relative path</em> location for our
 * application context.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see CoreContextConfigurationAppCtxTests
 * @see AbsolutePathContextConfigurationAppCtxTests
 */
@ContextConfiguration(locations = "CoreContextConfigurationAppCtxTests-context.xml", inheritLocations = false)
class RelativePathContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {
	/* all tests are in the parent class. */
}
