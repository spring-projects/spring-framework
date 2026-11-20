/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.initializers;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import org.springframework.context.ApplicationContextInitializer;

/**
 * JUnit Platform based test suite for tests that involve
 * {@link ApplicationContextInitializer ApplicationContextInitializers} (ACIs)
 * in the TestContext framework.
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
 * @since 3.2
 */
@Suite
@IncludeEngines("junit-jupiter")
@SelectPackages({
	"org.springframework.test.context.initializers.annotation",
	"org.springframework.test.context.initializers.xml"
})
@IncludeClassNamePatterns(".*Tests$")
@ExcludeTags("failing-test-case")
@ConfigurationParameter(
	key = ClassOrderer.DEFAULT_ORDER_PROPERTY_NAME,
	value = "org.junit.jupiter.api.ClassOrderer$ClassName"
)
class AciTestSuite {
}
