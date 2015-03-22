/*
 * Copyright 2002-2015 the original author or authors.
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
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.TestContext;
import org.springframework.util.Assert;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.*;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.*;

/**
 * {@code TestExecutionListener} which provides support for marking the
 * {@code ApplicationContext} associated with a test as <em>dirty</em> for
 * both test classes and test methods annotated with the
 * {@link DirtiesContext @DirtiesContext} annotation.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see DirtiesContext
 */
public class DirtiesContextTestExecutionListener extends AbstractTestExecutionListener {

	private static final Log logger = LogFactory.getLog(DirtiesContextTestExecutionListener.class);


	/**
	 * Returns {@code 3000}.
	 */
	@Override
	public final int getOrder() {
		return 3000;
	}

	/**
	 * If the test class of the supplied {@linkplain TestContext test context}
	 * is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#BEFORE_CLASS BEFORE_CLASS}, the {@linkplain ApplicationContext
	 * application context} of the test context will be
	 * {@linkplain TestContext#markApplicationContextDirty marked as dirty}, and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
	 * {@code true}.
	 */
	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		beforeOrAfterTestClass(testContext, "Before", BEFORE_CLASS);
	}

	/**
	 * If the current test method of the supplied {@linkplain TestContext test
	 * context} is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#methodMode() method mode} is set to {@link
	 * MethodMode#BEFORE_METHOD BEFORE_METHOD}, or if the test class is
	 * annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#BEFORE_EACH_TEST_METHOD BEFORE_EACH_TEST_METHOD}, the
	 * {@linkplain ApplicationContext application context} of the test context
	 * will be {@linkplain TestContext#markApplicationContextDirty marked as dirty} and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to {@code true}.
	 * @since 4.2
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		beforeOrAfterTestMethod(testContext, "Before", BEFORE_METHOD, BEFORE_EACH_TEST_METHOD);
	}

	/**
	 * If the current test method of the supplied {@linkplain TestContext test
	 * context} is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#methodMode() method mode} is set to {@link
	 * MethodMode#AFTER_METHOD AFTER_METHOD}, or if the test class is
	 * annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}, the
	 * {@linkplain ApplicationContext application context} of the test context
	 * will be {@linkplain TestContext#markApplicationContextDirty marked as dirty} and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to {@code true}.
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		beforeOrAfterTestMethod(testContext, "After", AFTER_METHOD, AFTER_EACH_TEST_METHOD);
	}

	/**
	 * If the test class of the supplied {@linkplain TestContext test context}
	 * is annotated with {@code @DirtiesContext} and the {@linkplain
	 * DirtiesContext#classMode() class mode} is set to {@link
	 * ClassMode#AFTER_CLASS AFTER_CLASS}, the {@linkplain ApplicationContext
	 * application context} of the test context will be
	 * {@linkplain TestContext#markApplicationContextDirty marked as dirty}, and the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
	 * {@code true}.
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		beforeOrAfterTestClass(testContext, "After", AFTER_CLASS);
	}

	/**
	 * Marks the {@linkplain ApplicationContext application context} of the supplied
	 * {@linkplain TestContext test context} as
	 * {@linkplain TestContext#markApplicationContextDirty(DirtiesContext.HierarchyMode) dirty}
	 * and sets {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
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
	 * Perform the actual work for {@link #beforeTestMethod} and {@link #afterTestMethod}.
	 * @since 4.2
	 */
	private void beforeOrAfterTestMethod(TestContext testContext, String phase, MethodMode requiredMethodMode,
			ClassMode requiredClassMode) throws Exception {
		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		final String annotationType = DirtiesContext.class.getName();
		AnnotationAttributes methodAnnAttrs = AnnotatedElementUtils.getAnnotationAttributes(testMethod, annotationType);
		AnnotationAttributes classAnnAttrs = AnnotatedElementUtils.getAnnotationAttributes(testClass, annotationType);
		boolean methodAnnotated = methodAnnAttrs != null;
		boolean classAnnotated = classAnnAttrs != null;
		MethodMode methodMode = methodAnnotated ? methodAnnAttrs.<MethodMode> getEnum("methodMode") : null;
		ClassMode classMode = classAnnotated ? classAnnAttrs.<ClassMode> getEnum("classMode") : null;

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(
				"%s test method: context %s, class annotated with @DirtiesContext [%s] with mode [%s], method annotated with @DirtiesContext [%s] with mode [%s].",
				phase, testContext, classAnnotated, classMode, methodAnnotated, methodMode));
		}

		if ((methodMode == requiredMethodMode) || (classMode == requiredClassMode)) {
			HierarchyMode hierarchyMode = methodAnnotated ? methodAnnAttrs.<HierarchyMode> getEnum("hierarchyMode")
					: classAnnAttrs.<HierarchyMode> getEnum("hierarchyMode");
			dirtyContext(testContext, hierarchyMode);
		}
	}

	/**
	 * Perform the actual work for {@link #beforeTestClass} and {@link #afterTestClass}.
	 * @since 4.2
	 */
	private void beforeOrAfterTestClass(TestContext testContext, String phase, ClassMode requiredClassMode)
			throws Exception {
		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");

		final String annotationType = DirtiesContext.class.getName();
		AnnotationAttributes classAnnAttrs = AnnotatedElementUtils.getAnnotationAttributes(testClass, annotationType);
		boolean classAnnotated = classAnnAttrs != null;
		ClassMode classMode = classAnnotated ? classAnnAttrs.<ClassMode> getEnum("classMode") : null;

		if (logger.isDebugEnabled()) {
			logger.debug(String.format(
				"%s test class: context %s, class annotated with @DirtiesContext [%s] with mode [%s].", phase,
				testContext, classAnnotated, classMode));
		}

		if (classMode == requiredClassMode) {
			HierarchyMode hierarchyMode = classAnnAttrs.<HierarchyMode> getEnum("hierarchyMode");
			dirtyContext(testContext, hierarchyMode);
		}
	}

}
