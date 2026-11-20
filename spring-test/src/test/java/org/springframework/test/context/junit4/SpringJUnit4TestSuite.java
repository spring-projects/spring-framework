/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit4;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * JUnit test suite for tests involving {@link SpringRunner} and the
 * <em>Spring TestContext Framework</em>; only intended to be run manually as a
 * convenience.
 *
 * <p>This test suite serves a dual purpose of verifying that tests run with
 * {@link SpringRunner} can be used in conjunction with JUnit's
 * {@link Suite} runner.
 *
 * <p>Note that tests included in this suite will be executed at least twice if
 * run from an automated build process, test runner, etc. that is not configured
 * to exclude tests based on a {@code "*TestSuite.class"} pattern match.
 *
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(Suite.class)
// Note: the following 'multi-line' layout is for enhanced code readability.
@SuiteClasses({
	StandardJUnit4FeaturesTests.class,
	StandardJUnit4FeaturesSpringRunnerTests.class,
	SpringJUnit47ClassRunnerRuleTests.class,
	ExpectedExceptionSpringRunnerTests.class,
	TimedSpringRunnerTests.class,
	RepeatedSpringRunnerTests.class,
	EnabledAndIgnoredSpringRunnerTests.class,
	HardCodedProfileValueSourceSpringRunnerTests.class,
	ParameterizedDependencyInjectionTests.class,
	ConcreteTransactionalJUnit4SpringContextTests.class,
	TimedTransactionalSpringRunnerTests.class
})
public class SpringJUnit4TestSuite {
	/* this test case consists entirely of tests loaded as a suite. */
}
