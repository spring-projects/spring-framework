/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an explicitly named properties file in the classpath.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@TestPropertySource("explicit.properties")
@DisabledInAotMode("Because ExplicitPropertiesFileTestPropertySourceTests is disabled in AOT mode")
public class ExplicitPropertiesFileInClasspathTestPropertySourceTests extends AbstractExplicitPropertiesFileTests {
}
