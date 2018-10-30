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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.mockito.BDDMockito;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.*;
import static org.springframework.test.annotation.DirtiesContext.HierarchyMode.*;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.*;

/**
 * Unit tests for {@link DirtiesContextBeforeModesTestExecutionListener}.
 * and {@link DirtiesContextTestExecutionListener}
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class DirtiesContextTestExecutionListenerTests {

	private final TestExecutionListener beforeListener = new DirtiesContextBeforeModesTestExecutionListener();
	private final TestExecutionListener afterListener = new DirtiesContextTestExecutionListener();
	private final TestContext testContext = mock(TestContext.class);


	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredLocallyOnMethodWithBeforeMethodMode() throws Exception {
		Class<?> clazz = getClass();
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(
			clazz.getDeclaredMethod("dirtiesContextDeclaredLocallyWithBeforeMethodMode"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredLocallyOnMethodWithAfterMethodMode() throws Exception {
		Class<?> clazz = getClass();
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(
			clazz.getDeclaredMethod("dirtiesContextDeclaredLocallyWithAfterMethodMode"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredOnMethodViaMetaAnnotationWithAfterMethodMode()
			throws Exception {
		Class<?> clazz = getClass();
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(
			clazz.getDeclaredMethod("dirtiesContextDeclaredViaMetaAnnotationWithAfterMethodMode"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredLocallyOnClassBeforeEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyBeforeEachTestMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("clean"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredLocallyOnClassAfterEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterEachTestMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("clean"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterEachTestMethod()
			throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("clean"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredLocallyOnClassBeforeClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyBeforeClass.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("clean"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredLocallyOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterClass.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("clean"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterClass.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("clean"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestMethodForDirtiesContextViaMetaAnnotationWithOverrides() throws Exception {
		Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverrides.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("clean"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(CURRENT_LEVEL);
	}

	// -------------------------------------------------------------------------

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredLocallyOnMethod() throws Exception {
		Class<?> clazz = getClass();
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredLocallyOnClassBeforeEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyBeforeEachTestMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredLocallyOnClassAfterEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterEachTestMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterEachTestMethod()
			throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredLocallyOnClassBeforeClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyBeforeClass.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredLocallyOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterClass.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterClass.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredViaMetaAnnotationWithOverrides() throws Exception {
		Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverrides.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
	}

	@Test
	public void beforeAndAfterTestClassForDirtiesContextDeclaredViaMetaAnnotationWithOverridenAttributes()
			throws Exception {
		Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverridenAttributes.class;
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	// -------------------------------------------------------------------------

	@DirtiesContext(methodMode = BEFORE_METHOD)
	void dirtiesContextDeclaredLocallyWithBeforeMethodMode() {
		/* no-op */
	}

	@DirtiesContext
	void dirtiesContextDeclaredLocallyWithAfterMethodMode() {
		/* no-op */
	}

	@MetaDirtyAfterMethod
	void dirtiesContextDeclaredViaMetaAnnotationWithAfterMethodMode() {
		/* no-op */
	}


	@DirtiesContext
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaDirtyAfterMethod {
	}

	@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaDirtyAfterEachTestMethod {
	}

	@DirtiesContext(classMode = AFTER_CLASS)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaDirtyAfterClass {
	}

	@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
	static class DirtiesContextDeclaredLocallyBeforeEachTestMethod {

		void clean() {
			/* no-op */
		}
	}

	@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
	static class DirtiesContextDeclaredLocallyAfterEachTestMethod {

		void clean() {
			/* no-op */
		}
	}

	@DirtiesContext
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaDirtyWithOverrides {

		ClassMode classMode() default AFTER_EACH_TEST_METHOD;

		HierarchyMode hierarchyMode() default HierarchyMode.CURRENT_LEVEL;
	}

	@MetaDirtyAfterEachTestMethod
	static class DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod {

		void clean() {
			/* no-op */
		}
	}

	@DirtiesContext(classMode = BEFORE_CLASS)
	static class DirtiesContextDeclaredLocallyBeforeClass {

		void clean() {
			/* no-op */
		}
	}

	@DirtiesContext(classMode = AFTER_CLASS)
	static class DirtiesContextDeclaredLocallyAfterClass {

		void clean() {
			/* no-op */
		}
	}

	@MetaDirtyAfterClass
	static class DirtiesContextDeclaredViaMetaAnnotationAfterClass {

		void clean() {
			/* no-op */
		}
	}

	@MetaDirtyWithOverrides
	static class DirtiesContextViaMetaAnnotationWithOverrides {

		void clean() {
			/* no-op */
		}
	}

	@MetaDirtyWithOverrides(classMode = AFTER_CLASS, hierarchyMode = EXHAUSTIVE)
	static class DirtiesContextViaMetaAnnotationWithOverridenAttributes {

		void clean() {
			/* no-op */
		}
	}

}
