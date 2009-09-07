/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.test.context.ClassLevelDirtiesContextTests;
import org.springframework.test.context.SpringRunnerContextCacheTests;
import org.springframework.test.context.junit4.orm.HibernateSessionFlushingTests;

/**
 * <p>
 * JUnit 4 based test suite for tests involving {@link SpringJUnit4ClassRunner}
 * and the <em>Spring TestContext Framework</em>.
 * </p>
 * <p>
 * This test suite serves a dual purpose of verifying that tests run with
 * {@link SpringJUnit4ClassRunner} can be used in conjunction with JUnit 4's
 * {@link Suite} runner.
 * </p>
 * <p>
 * Note that tests included in this suite will be executed at least twice if run
 * from an automated build process, test runner, etc. that is configured to run
 * tests based on a &quot;*Tests.class&quot; pattern match.
 * </p>
 * 
 * @author Sam Brannen
 * @since 2.5
 */
@RunWith(Suite.class)
// Note: the following 'multi-line' layout is for enhanced code readability.
@SuiteClasses( {//
StandardJUnit4FeaturesTests.class,//
	StandardJUnit4FeaturesSpringRunnerTests.class,//
	SpringJUnit47ClassRunnerRuleTests.class,//
	ExpectedExceptionSpringRunnerTests.class,//
	TimedSpringRunnerTests.class,//
	RepeatedSpringRunnerTests.class,//
	EnabledAndIgnoredSpringRunnerTests.class,//
	HardCodedProfileValueSourceSpringRunnerTests.class,//
	SpringJUnit4ClassRunnerAppCtxTests.class,//
	ClassPathResourceSpringJUnit4ClassRunnerAppCtxTests.class,//
	AbsolutePathSpringJUnit4ClassRunnerAppCtxTests.class,//
	RelativePathSpringJUnit4ClassRunnerAppCtxTests.class,//
	MultipleResourcesSpringJUnit4ClassRunnerAppCtxTests.class,//
	InheritedConfigSpringJUnit4ClassRunnerAppCtxTests.class,//
	PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests.class,//
	CustomDefaultContextLoaderClassSpringRunnerTests.class,//
	SpringRunnerContextCacheTests.class,//
	ClassLevelDirtiesContextTests.class,//
	ParameterizedDependencyInjectionTests.class,//
	ClassLevelTransactionalSpringRunnerTests.class,//
	MethodLevelTransactionalSpringRunnerTests.class,//
	DefaultRollbackTrueTransactionalSpringRunnerTests.class,//
	DefaultRollbackFalseTransactionalSpringRunnerTests.class,//
	RollbackOverrideDefaultRollbackTrueTransactionalSpringRunnerTests.class,//
	RollbackOverrideDefaultRollbackFalseTransactionalSpringRunnerTests.class,//
	BeforeAndAfterTransactionAnnotationTests.class,//
	TimedTransactionalSpringRunnerTests.class,//
	HibernateSessionFlushingTests.class //
})
public class SpringJUnit4SuiteTests {
	/* this test case consists entirely of tests loaded as a suite. */
}
