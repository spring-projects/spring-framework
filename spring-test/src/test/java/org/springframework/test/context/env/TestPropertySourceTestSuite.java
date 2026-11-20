/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import org.springframework.test.context.TestPropertySource;

/**
 * Test suite for tests that involve {@link TestPropertySource @TestPropertySource}.
 *
 * <p>Note that tests included in this suite will be executed at least twice if
 * run from an automated build process, test runner, etc. that is not configured
 * to exclude tests based on a {@code "*TestSuite.class"} pattern match.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@Suite
@IncludeEngines("junit-jupiter")
@SelectPackages("org.springframework.test.context.env")
@IncludeClassNamePatterns(".*Tests$")
class TestPropertySourceTestSuite {
}
