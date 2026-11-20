/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform based test suite for tests in the spring-expression module.
 *
 * <p><strong>This suite is only intended to be used manually within an IDE.</strong>
 *
 * @author Sam Brannen
 */
@Suite
@SelectPackages("org.springframework.expression.spel")
@IncludeClassNamePatterns(".*Tests?$")
class SpringExpressionTestSuite {
}
