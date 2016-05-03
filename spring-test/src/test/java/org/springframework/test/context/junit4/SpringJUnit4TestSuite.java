/*
 * Copyright 2002-2016 the original author or authors.
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
import org.springframework.test.context.cache.ClassLevelDirtiesContextTests;
import org.springframework.test.context.cache.SpringRunnerContextCacheTests;
import org.springframework.test.context.jdbc.RequiresNewTransactionSqlScriptsTests;
import org.springframework.test.context.junit4.annotation.AnnotationConfigSpringJUnit4ClassRunnerAppCtxTests;
import org.springframework.test.context.junit4.annotation.BeanOverridingDefaultConfigClassesInheritedTests;
import org.springframework.test.context.junit4.annotation.BeanOverridingExplicitConfigClassesInheritedTests;
import org.springframework.test.context.junit4.annotation.DefaultConfigClassesBaseTests;
import org.springframework.test.context.junit4.annotation.DefaultConfigClassesInheritedTests;
import org.springframework.test.context.junit4.annotation.DefaultLoaderBeanOverridingDefaultConfigClassesInheritedTests;
import org.springframework.test.context.junit4.annotation.DefaultLoaderBeanOverridingExplicitConfigClassesInheritedTests;
import org.springframework.test.context.junit4.annotation.DefaultLoaderDefaultConfigClassesBaseTests;
import org.springframework.test.context.junit4.annotation.DefaultLoaderDefaultConfigClassesInheritedTests;
import org.springframework.test.context.junit4.annotation.DefaultLoaderExplicitConfigClassesBaseTests;
import org.springframework.test.context.junit4.annotation.DefaultLoaderExplicitConfigClassesInheritedTests;
import org.springframework.test.context.junit4.annotation.ExplicitConfigClassesBaseTests;
import org.springframework.test.context.junit4.annotation.ExplicitConfigClassesInheritedTests;
import org.springframework.test.context.junit4.orm.HibernateSessionFlushingTests;
import org.springframework.test.context.junit4.profile.annotation.DefaultProfileAnnotationConfigTests;
import org.springframework.test.context.junit4.profile.annotation.DevProfileAnnotationConfigTests;
import org.springframework.test.context.junit4.profile.annotation.DevProfileResolverAnnotationConfigTests;
import org.springframework.test.context.junit4.profile.xml.DefaultProfileXmlConfigTests;
import org.springframework.test.context.junit4.profile.xml.DevProfileResolverXmlConfigTests;
import org.springframework.test.context.junit4.profile.xml.DevProfileXmlConfigTests;
import org.springframework.test.context.transaction.programmatic.ProgrammaticTxMgmtTests;

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
@SuiteClasses({//
StandardJUnit4FeaturesTests.class,//
	StandardJUnit4FeaturesSpringRunnerTests.class,//
	SpringJUnit47ClassRunnerRuleTests.class,//
	AnnotationConfigSpringJUnit4ClassRunnerAppCtxTests.class,//
	DefaultConfigClassesBaseTests.class,//
	DefaultConfigClassesInheritedTests.class,//
	BeanOverridingDefaultConfigClassesInheritedTests.class,//
	ExplicitConfigClassesBaseTests.class,//
	ExplicitConfigClassesInheritedTests.class,//
	BeanOverridingExplicitConfigClassesInheritedTests.class,//
	DefaultLoaderDefaultConfigClassesBaseTests.class,//
	DefaultLoaderDefaultConfigClassesInheritedTests.class,//
	DefaultLoaderBeanOverridingDefaultConfigClassesInheritedTests.class,//
	DefaultLoaderExplicitConfigClassesBaseTests.class,//
	DefaultLoaderExplicitConfigClassesInheritedTests.class,//
	DefaultLoaderBeanOverridingExplicitConfigClassesInheritedTests.class,//
	DefaultProfileAnnotationConfigTests.class,//
	DevProfileAnnotationConfigTests.class,//
	DevProfileResolverAnnotationConfigTests.class,//
	DefaultProfileXmlConfigTests.class,//
	DevProfileXmlConfigTests.class,//
	DevProfileResolverXmlConfigTests.class,//
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
	ConcreteTransactionalJUnit4SpringContextTests.class,//
	ClassLevelTransactionalSpringRunnerTests.class,//
	MethodLevelTransactionalSpringRunnerTests.class,//
	DefaultRollbackTrueTransactionalTests.class,//
	DefaultRollbackFalseTransactionalTests.class,//
	RollbackOverrideDefaultRollbackTrueTransactionalTests.class,//
	RollbackOverrideDefaultRollbackFalseTransactionalTests.class,//
	BeforeAndAfterTransactionAnnotationTests.class,//
	TimedTransactionalSpringRunnerTests.class,//
	ProgrammaticTxMgmtTests.class,//
	RequiresNewTransactionSqlScriptsTests.class,//
	HibernateSessionFlushingTests.class //
})
public class SpringJUnit4TestSuite {
	/* this test case consists entirely of tests loaded as a suite. */
}
