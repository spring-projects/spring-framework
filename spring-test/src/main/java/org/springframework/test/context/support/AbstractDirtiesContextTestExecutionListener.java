/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.support;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.util.Assert;

/**
 * Abstract base class for {@code TestExecutionListener} implementations that
 * provide support for marking the {@code ApplicationContext} associated with
 * a test as <em>dirty</em> for both test classes and test methods annotated
 * with the {@link DirtiesContext @DirtiesContext} annotation.
 *
 * <p>The core functionality for this class was extracted from
 * {@link DirtiesContextTestExecutionListener} in Spring Framework 4.2.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.2
 * @see DirtiesContext
 */
public abstract class AbstractDirtiesContextTestExecutionListener extends AbstractTestExecutionListener {

	private final Log logger = LogFactory.getLog(getClass());


	@Override
	public abstract int getOrder();

	/**
	 * Mark the {@linkplain ApplicationContext application context} of the supplied
	 * {@linkplain TestContext test context} as
	 * {@linkplain TestContext#markApplicationContextDirty(DirtiesContext.HierarchyMode) dirty}
	 * and set {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context to {@code true}.
	 * @param testContext the test context whose application context should
	 * be marked as dirty
	 * @param hierarchyMode the context cache clearing mode to be applied if the
	 * context is part of a hierarchy; may be {@code null}
	 * @since 3.2.2
	 */
	protected void dirtyContext(TestContext testContext, @Nullable HierarchyMode hierarchyMode) {
		testContext.markApplicationContextDirty(hierarchyMode);
		testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE, Boolean.TRUE);
	}

	/**
	 * Perform the actual work for {@link #beforeTestMethod} and {@link #afterTestMethod}
	 * by dirtying the context if appropriate (i.e., according to the required modes).
	 * @param testContext the test context whose application context should
	 * potentially be marked as dirty; never {@code null}
	 * @param requiredMethodMode the method mode required for a context to
	 * be marked dirty in the current phase; never {@code null}
	 * @param requiredClassMode the class mode required for a context to
	 * be marked dirty in the current phase; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 4.2
	 * @see #dirtyContext
	 */
	protected void beforeOrAfterTestMethod(TestContext testContext, MethodMode requiredMethodMode,
			ClassMode requiredClassMode) throws Exception {

		Assert.notNull(testContext, "TestContext must not be null");
		Assert.notNull(requiredMethodMode, "requiredMethodMode must not be null");
		Assert.notNull(requiredClassMode, "requiredClassMode must not be null");

		Class<?> testClass = testContext.getTestClass();
		Method testMethod = testContext.getTestMethod();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");
		Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

		DirtiesContext methodAnn = AnnotatedElementUtils.findMergedAnnotation(testMethod, DirtiesContext.class);
		DirtiesContext classAnn = TestContextAnnotationUtils.findMergedAnnotation(testClass, DirtiesContext.class);
		boolean methodAnnotated = (methodAnn != null);
		boolean classAnnotated = (classAnn != null);
		MethodMode methodMode = (methodAnnotated ? methodAnn.methodMode() : null);
		ClassMode classMode = (classAnnotated ? classAnn.classMode() : null);

		if (logger.isTraceEnabled()) {
			logger.trace("""
					%s test method: context %s, class annotated with @DirtiesContext [%s] \
					with mode [%s], method annotated with @DirtiesContext [%s] with mode [%s]"""
						.formatted(getPhase(requiredMethodMode), testContext, classAnnotated, classMode,
							methodAnnotated, methodMode));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("""
					%s test method: class [%s], method [%s], class annotated with @DirtiesContext [%s] \
					with mode [%s], method annotated with @DirtiesContext [%s] with mode [%s]"""
						.formatted(getPhase(requiredMethodMode), testClass.getSimpleName(),
							testMethod.getName(), classAnnotated, classMode, methodAnnotated, methodMode));
		}

		if ((methodMode == requiredMethodMode) || (classMode == requiredClassMode)) {
			HierarchyMode hierarchyMode = (methodAnnotated ? methodAnn.hierarchyMode() : classAnn.hierarchyMode());
			dirtyContext(testContext, hierarchyMode);
		}
	}

	/**
	 * Perform the actual work for {@link #beforeTestClass} and {@link #afterTestClass}
	 * by dirtying the context if appropriate (i.e., according to the required mode).
	 * @param testContext the test context whose application context should
	 * potentially be marked as dirty; never {@code null}
	 * @param requiredClassMode the class mode required for a context to
	 * be marked dirty in the current phase; never {@code null}
	 * @throws Exception allows any exception to propagate
	 * @since 4.2
	 * @see #dirtyContext
	 */
	protected void beforeOrAfterTestClass(TestContext testContext, ClassMode requiredClassMode) throws Exception {
		Assert.notNull(testContext, "TestContext must not be null");
		Assert.notNull(requiredClassMode, "requiredClassMode must not be null");

		Class<?> testClass = testContext.getTestClass();
		Assert.notNull(testClass, "The test class of the supplied TestContext must not be null");

		DirtiesContext dirtiesContext = TestContextAnnotationUtils.findMergedAnnotation(testClass, DirtiesContext.class);
		boolean classAnnotated = (dirtiesContext != null);
		ClassMode classMode = (classAnnotated ? dirtiesContext.classMode() : null);

		if (logger.isTraceEnabled()) {
			logger.trace("%s test class: context %s, class annotated with @DirtiesContext [%s] with mode [%s]"
					.formatted(getPhase(requiredClassMode), testContext, classAnnotated, classMode));
		}
		else if (logger.isDebugEnabled()) {
			logger.debug("%s test class: class [%s], class annotated with @DirtiesContext [%s] with mode [%s]"
					.formatted(getPhase(requiredClassMode), testClass.getSimpleName(), classAnnotated, classMode));
		}

		if (classMode == requiredClassMode) {
			dirtyContext(testContext, dirtiesContext.hierarchyMode());
		}
	}

	private static String getPhase(ClassMode classMode) {
		return (classMode.name().startsWith("BEFORE") ? "Before" : "After");
	}

	private static String getPhase(MethodMode methodMode) {
		return (methodMode.name().startsWith("BEFORE") ? "Before" : "After");
	}

}
