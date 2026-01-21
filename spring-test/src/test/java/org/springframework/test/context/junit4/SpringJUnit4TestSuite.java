/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
