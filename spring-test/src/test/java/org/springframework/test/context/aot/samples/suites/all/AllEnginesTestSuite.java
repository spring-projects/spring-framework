/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.suites.all;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@Suite
@SelectPackages("org.springframework.test.context.aot.samples.basic")
public class AllEnginesTestSuite {
}
