/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an inherited explicitly named properties file that is
 * referenced using a relative path.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@DisabledInAotMode("Because ExplicitPropertiesFileTestPropertySourceTests is disabled in AOT mode")
class InheritedRelativePathPropertiesFileTestPropertySourceTests extends
		ExplicitPropertiesFileInClasspathTestPropertySourceTests {

	/* all tests are in superclass */

}
