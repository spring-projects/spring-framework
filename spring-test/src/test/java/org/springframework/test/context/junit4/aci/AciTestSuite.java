/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.junit4.aci;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.test.context.junit4.aci.annotation.InitializerWithoutConfigFilesOrClassesTest;
import org.springframework.test.context.junit4.aci.annotation.MergedInitializersAnnotationConfigTests;
import org.springframework.test.context.junit4.aci.annotation.MultipleInitializersAnnotationConfigTests;
import org.springframework.test.context.junit4.aci.annotation.OrderedInitializersAnnotationConfigTests;
import org.springframework.test.context.junit4.aci.annotation.OverriddenInitializersAnnotationConfigTests;
import org.springframework.test.context.junit4.aci.annotation.SingleInitializerAnnotationConfigTests;
import org.springframework.test.context.junit4.aci.xml.MultipleInitializersXmlConfigTests;

/**
 * Convenience test suite for integration tests that verify support for
 * {@link ApplicationContextInitializer ApplicationContextInitializers} (ACIs)
 * in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 3.2
 */
@RunWith(Suite.class)
// Note: the following 'multi-line' layout is for enhanced code readability.
@SuiteClasses({//
	MultipleInitializersXmlConfigTests.class,//
	SingleInitializerAnnotationConfigTests.class,//
	MultipleInitializersAnnotationConfigTests.class,//
	MergedInitializersAnnotationConfigTests.class,//
	OverriddenInitializersAnnotationConfigTests.class,//
	OrderedInitializersAnnotationConfigTests.class,//
	InitializerWithoutConfigFilesOrClassesTest.class //
})
public class AciTestSuite {
}
