/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.test.context.junit4.EnabledAndIgnoredSpringRunnerTests;

/**
 * This class is an extension of {@link EnabledAndIgnoredSpringRunnerTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
@SuppressWarnings("deprecation")
public class EnabledAndIgnoredSpringRuleTests extends EnabledAndIgnoredSpringRunnerTests {

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	// All tests are in superclass.

}
