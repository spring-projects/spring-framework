/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config;

import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests that verify support for configuration classes in
 * the Spring TestContext Framework.
 *
 * <p>Furthermore, by extending {@link CoreContextConfigurationAppCtxTests},
 * this class also verifies support for several basic features of the
 * Spring TestContext Framework. See JavaDoc in
 * {@link CoreContextConfigurationAppCtxTests} for details.
 *
 * <p>Configuration will be loaded from {@link PojoAndStringConfig}.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@ContextConfiguration(classes = PojoAndStringConfig.class, inheritLocations = false)
class AnnotationConfigContextConfigurationAppCtxTests extends CoreContextConfigurationAppCtxTests {
	/* all tests are in the parent class. */
}
