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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;

import static org.mockito.Mockito.*;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.*;
import static org.springframework.test.annotation.DirtiesContext.HierarchyMode.*;

/**
 * Unit tests for {@link DirtiesContextTestExecutionListener}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public class DirtiesContextTestExecutionListenerTests {

	private final DirtiesContextTestExecutionListener listener = new DirtiesContextTestExecutionListener();
	private final TestContext testContext = mock(TestContext.class);


	@Test
	public void afterTestMethodForDirtiesContextDeclaredLocallyOnMethod() throws Exception {
		Class<?> clazz = getClass();
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("dirtiesContextDeclaredLocally"));
		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestMethodForDirtiesContextDeclaredOnMethodViaMetaAnnotation() throws Exception {
		Class<?> clazz = getClass();
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("dirtiesContextDeclaredViaMetaAnnotation"));
		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestMethodForDirtiesContextDeclaredLocallyOnClassAfterEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterEachTestMethod.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("clean"));
		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestMethodForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("clean"));
		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestMethodForDirtiesContextDeclaredLocallyOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterClass.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("clean"));
		listener.afterTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestMethodForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterClass.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("clean"));
		listener.afterTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestMethodForDirtiesContextViaMetaAnnotationWithOverrides() throws Exception {
		Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverrides.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		when(testContext.getTestMethod()).thenReturn(clazz.getDeclaredMethod("clean"));
		listener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(CURRENT_LEVEL);
	}

	// -------------------------------------------------------------------------

	@Test
	public void afterTestClassForDirtiesContextDeclaredLocallyOnMethod() throws Exception {
		Class<?> clazz = getClass();
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		listener.afterTestClass(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestClassForDirtiesContextDeclaredLocallyOnClassAfterEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterEachTestMethod.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		listener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestClassForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterEachTestMethod() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		listener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestClassForDirtiesContextDeclaredLocallyOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredLocallyAfterClass.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		listener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestClassForDirtiesContextDeclaredViaMetaAnnotationOnClassAfterClass() throws Exception {
		Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterClass.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		listener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	@Test
	public void afterTestClassForDirtiesContextDeclaredViaMetaAnnotationWithOverrides() throws Exception {
		Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverrides.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		listener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(CURRENT_LEVEL);
	}

	@Test
	public void afterTestClassForDirtiesContextDeclaredViaMetaAnnotationWithOverridenAttributes() throws Exception {
		Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverridenAttributes.class;
		Mockito.<Class<?>> when(testContext.getTestClass()).thenReturn(clazz);
		listener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	// -------------------------------------------------------------------------

	@DirtiesContext
	void dirtiesContextDeclaredLocally() {
		/* no-op */
	}

	@MetaDirty
	void dirtiesContextDeclaredViaMetaAnnotation() {
		/* no-op */
	}


	@DirtiesContext
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaDirty {
	}

	@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaDirtyAfterEachTestMethod {
	}

	@DirtiesContext(classMode = AFTER_CLASS)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaDirtyAfterClass {
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
