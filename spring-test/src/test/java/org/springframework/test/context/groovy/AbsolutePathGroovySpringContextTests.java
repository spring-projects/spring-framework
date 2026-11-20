/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.groovy;

import org.springframework.test.context.ContextConfiguration;

/**
 * Extension of {@link GroovySpringContextTests} that declares a Groovy
 * script using an absolute path.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see GroovySpringContextTests
 * @see RelativePathGroovySpringContextTests
 */
@ContextConfiguration(locations = "/org/springframework/test/context/groovy/context.groovy", inheritLocations = false)
class AbsolutePathGroovySpringContextTests extends GroovySpringContextTests {

	/* all tests are in the superclass */

}
