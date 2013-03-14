/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.test.context.support;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;

/**
 * {@code TestExecutionListener} which provides support for marking the
 * {@code ApplicationContext} associated with a test as <em>dirty</em> for
 * both test classes and test methods configured with the {@link DirtiesContext
 * &#064;DirtiesContext} annotation.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see DirtiesContext
 */
public class DirtiesContextTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(DirtiesContextTestExecutionListener.class);


	/**
	 * Marks the {@linkplain ApplicationContext application context} of the supplied
	 * {@linkplain TestContext test context} as
	 * {@linkplain TestContext#markApplicationContextDirty() dirty}, and sets the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context to {@code true}.
	 * @param testContext the test context whose application context should
	 * marked as dirty
	 * @deprecated as of Spring 3.2.2, use {@link #dirtyContext(TestContext, DirtiesContext.HierarchyMode)} instead.
	 */
	@Deprecated
	protected void dirtyContext(TestContext testContext) {
		testContext.markApplicationContextDirty();
		testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE, Boolean.TRUE);
	}

	/**
	 * Marks the {@linkplain ApplicationContext application context} of the supplied
	 * {@linkplain TestContext test context} as {@linkplain
	 * TestContext#markApplicationContextDirty(DirtiesContext.HierarchyMode) dirty} and sets the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context to {@code true}.
	 * @param testContext the test context whose application context should
	 * marked as dirty
	 * @param hierarchyMode the context cache clearing mode to be applied if the
	 * context is part of a hierarchy; may be {@code null}
	 * @since 3.2.2
	 */
	protected void dirtyContext(TestContext testContext, HierarchyMode hierarchyMode) {
		testContext.markApplicationContextDirty(hierarchyMode);
		testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE, Boolean.TRUE);
	}

	/**
	 * If the current test method of the supplied {@linkplain TestContext test
	 * context} is annotated with {@link DirtiesContext &#064;DirtiesContext},
	 * or if the test class is annotated with {@link DirtiesContext
	 * &#064;DirtiesContext} and the {@linkplain DirtiesContext#classMode() class
	 * mode} is set to {@link ClassMode#AFTER_EACH_TEST_METHOD
	 * AFTER_EACH_TEST_METHOD}, the {@linkplain ApplicationContext application
	 * context} of the test context will be
	 * {@linkplain TestContext#markApplicationContextDirty() marked as dirty} and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
	 * {@code true}.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		final Class<DirtiesContext> annotationType = DirtiesContext.class;

		boolean methodDirtiesContext = testMethod.isAnnotationPresent(annotationType);
		boolean classDirtiesContext = testClass.isAnnotationPresent(annotationType);
		DirtiesContext classDirtiesContextAnnotation = testClass.getAnnotation(annotationType);
		ClassMode classMode = classDirtiesContext ? classDirtiesContextAnnotation.classMode() : null;

		if (logger.isDebugEnabled()) {
			logger.debug("After test method: context [" + testContext + "], class dirties context ["
					+ classDirtiesContext + "], class mode [" + classMode + "], method dirties context ["
					+ methodDirtiesContext + "].");
		}

		if (methodDirtiesContext || (classDirtiesContext && classMode == ClassMode.AFTER_EACH_TEST_METHOD)) {
			HierarchyMode hierarchyMode = methodDirtiesContext ? testMethod.getAnnotation(annotationType).hierarchyMode()
					: classDirtiesContextAnnotation.hierarchyMode();
			dirtyContext(testContext, hierarchyMode);
		}
	}

	/**
	 * If the test class of the supplied {@linkplain TestContext test context} is
	 * annotated with {@link DirtiesContext &#064;DirtiesContext}, the
	 * {@linkplain ApplicationContext application context} of the test context will
	 * be {@linkplain TestContext#markApplicationContextDirty() marked as dirty} ,
	 * and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
	 * {@code true}.
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");

		final Class<DirtiesContext> annotationType = DirtiesContext.class;

		boolean dirtiesContext = testClass.isAnnotationPresent(annotationType);
		if (logger.isDebugEnabled()) {
			logger.debug("After test class: context [" + testContext + "], dirtiesContext [" + dirtiesContext + "].");
		}
		if (dirtiesContext) {
			HierarchyMode hierarchyMode = testClass.getAnnotation(annotationType).hierarchyMode();
			dirtyContext(testContext, hierarchyMode);
		}
	}

}
