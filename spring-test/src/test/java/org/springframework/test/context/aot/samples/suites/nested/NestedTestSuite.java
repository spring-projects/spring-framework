/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.suites.nested;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@Suite
@IncludeClassNamePatterns(".*Suite$")
@SelectPackages({
	"org.springframework.test.context.aot.samples.suites.jupiter",
	"org.springframework.test.context.aot.samples.suites.vintage"
})
public class NestedTestSuite {
}
