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

package org.springframework.test.context;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.JUnitCoreUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.*;

/**
 * Verify the parent context exists when test class is annotated with {@link ParentContextConfiguration}.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */
@RunWith(Parameterized.class)
public class ParentContextConfigurationTests {

	private Class<? extends Test> testClass;

	@Parameters
	public static Collection<Object[]> classesToTestData() {
		return Arrays.asList(new Object[][]{{ChildWithAnnotationExtendingAnnotatedParentTestClass.class},
				{ChildWithoutAnnotationExtendingAnnotatedParentTestClass.class},
				{ChildWithAnnotationExtendingNonAnnotatedParentTestClass.class},
				{ChildWithoutAnnotationExtendingNonAnnotatedParentTestClass.class},});
	}

	public ParentContextConfigurationTests(Class<? extends Test> testClass) {
		this.testClass = testClass;
	}

	@Test
	public void testParentContext() {
		JUnitCore jUnitCore = new JUnitCore();
		Result result = jUnitCore.run(testClass);
		JUnitCoreUtils.verifyResult(result);
	}

	@RunWith(SpringJUnit4ClassRunner.class)
	@ContextConfiguration(classes = {ChildConfig.class})
	public static abstract class BaseTestClass {

		@Autowired
		protected ApplicationContext applicationContext;

	}

	/** parent test class which does NOT have annotation. */
	public static abstract class ParentWithoutAnnotationTestClass extends BaseTestClass {

	}

	/** parent test class which has annotation. */
	@ParentContextConfiguration(classes = {ParentConfig.class})
	public static abstract class ParentWithAnnotationTestClass extends BaseTestClass {

	}

	/** child has annotation, parent has annotation. */
	@ParentContextConfiguration(classes = {ParentConfig.class})
	public static class ChildWithAnnotationExtendingAnnotatedParentTestClass extends ParentWithAnnotationTestClass {

		@Test
		public void test() {
			verifyParentContextExist(applicationContext, true);
		}
	}

	/** child does NOT have annotation, parent has annotation. */
	public static class ChildWithoutAnnotationExtendingAnnotatedParentTestClass extends ParentWithAnnotationTestClass {

		@Test
		public void test() {
			verifyParentContextExist(applicationContext, true);
		}
	}

	/** child has annotation, parent does NOT have annotation. */
	@ParentContextConfiguration(classes = {ParentConfig.class})
	public static class ChildWithAnnotationExtendingNonAnnotatedParentTestClass
			extends ParentWithoutAnnotationTestClass {

		@Test
		public void test() {
			verifyParentContextExist(applicationContext, true);
		}
	}

	/** child does NOT have annotation, parent does NOT have annotation. */
	public static class ChildWithoutAnnotationExtendingNonAnnotatedParentTestClass
			extends ParentWithoutAnnotationTestClass {

		@Test
		public void test() {
			verifyParentContextExist(applicationContext, false);
		}
	}

	private static void verifyParentContextExist(ApplicationContext childContext, boolean exist) {
		assertThat("Child context must exist.", childContext, notNullValue());

		assertThat("Child context has local fooChild bean", childContext.containsLocalBean("fooChild"), is(true));

		ApplicationContext parentContext = childContext.getParent();
		if (exist) {
			assertThat("Parent context must exist.", parentContext, is(not(nullValue())));
			assertThat("Parent context has local fooParent bean", parentContext.containsLocalBean("fooParent"),
					is(true));
		}
		else {
			assertThat("Parent context must NOT exist.", parentContext, is(nullValue()));
		}
	}

	@Configuration
	public static class ParentConfig {

		@Bean(name = "fooParent")
		public String foo() {
			return "parent";
		}
	}

	@Configuration
	public static class ChildConfig {

		@Bean(name = "fooChild")
		public String foo() {
			return "child";
		}

	}


}
