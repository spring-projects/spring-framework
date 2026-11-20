/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit;

import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform based test suite for tests that involve the Spring TestContext
 * Framework and JUnit Jupiter.
 *
 * <p><strong>This suite is only intended to be used manually within an IDE.</strong>
 *
 * <h3>Logging Configuration</h3>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre style="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @author Sam Brannen
 * @since 5.0
 */
@Suite
@IncludeEngines("junit-jupiter")
@SelectPackages("org.springframework.test.context.junit.jupiter")
@IncludeClassNamePatterns(".*Tests$")
@ExcludeTags("failing-test-case")
class SpringJUnitJupiterTestSuite {
}
