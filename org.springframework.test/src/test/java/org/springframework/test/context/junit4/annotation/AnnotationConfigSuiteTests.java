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
 * TODO [SPR-6184] Document tests.
 *
 * @author Sam Brannen
 * @since 3.1
 */
@RunWith(Suite.class)
// Note: the following 'multi-line' layout is for enhanced code readability.
@SuiteClasses({

AnnotationConfigSpringJUnit4ClassRunnerAppCtxTests.class,

DefaultConfigClassBaseTests.class

// TODO Uncomment once working. Note that JUnit's Suite runner apparently
// does not heed the presence of @Ignore on a suite class, at least not
// when run within STS 2.6.0.
//
// DefaultConfigClassInheritedTests.class

})
public class AnnotationConfigSuiteTests {
}
