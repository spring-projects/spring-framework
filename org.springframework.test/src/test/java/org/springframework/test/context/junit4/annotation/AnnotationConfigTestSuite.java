/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context.junit4.annotation;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * JUnit test suite for annotation-driven <em>configuration class</em>
 * support in the Spring TestContext Framework.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@RunWith(Suite.class)
// Note: the following 'multi-line' layout is for enhanced code readability.
@SuiteClasses({//
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
	DefaultLoaderBeanOverridingExplicitConfigClassesInheritedTests.class //
})
public class AnnotationConfigTestSuite {
}
