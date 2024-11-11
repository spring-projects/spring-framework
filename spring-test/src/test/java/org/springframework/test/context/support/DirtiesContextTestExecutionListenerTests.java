/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;
import static org.springframework.test.annotation.DirtiesContext.HierarchyMode.CURRENT_LEVEL;
import static org.springframework.test.annotation.DirtiesContext.HierarchyMode.EXHAUSTIVE;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Tests for {@link DirtiesContextBeforeModesTestExecutionListener}.
 * and {@link DirtiesContextTestExecutionListener}
 *
 * @author Sam Brannen
 * @since 4.0
 */
@DisplayName("@DirtiesContext TestExecutionListener tests")
class DirtiesContextTestExecutionListenerTests {

	private final TestExecutionListener beforeListener = new DirtiesContextBeforeModesTestExecutionListener();
	private final TestExecutionListener afterListener = new DirtiesContextTestExecutionListener();
	private final TestContext testContext = mock();


	@Nested
	@DisplayName("Before and after test method")
	class BeforeAndAfterTestMethodTests {

		@Test
		void declaredLocallyOnMethodWithBeforeMethodMode() throws Exception {
			Class<?> clazz = getClass().getEnclosingClass();
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
		void declaredLocallyOnMethodWithAfterMethodMode() throws Exception {
			Class<?> clazz = getClass().getEnclosingClass();
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
		void declaredOnMethodViaMetaAnnotationWithAfterMethodMode() throws Exception {
			Class<?> clazz = getClass().getEnclosingClass();
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
		void declaredLocallyOnClassBeforeEachTestMethod() throws Exception {
			assertBeforeMethod(DirtiesContextDeclaredLocallyBeforeEachTestMethod.class);
		}

		@Test
		void declaredLocallyOnClassAfterEachTestMethod() throws Exception {
			assertAfterMethod(DirtiesContextDeclaredLocallyAfterEachTestMethod.class);
		}

		@Test
		void declaredViaMetaAnnotationOnClassAfterEachTestMethod() throws Exception {
			assertAfterMethod(DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod.class);
		}

		@Test
		void declaredLocallyOnClassBeforeClass() throws Exception {
			Class<?> clazz = DirtiesContextDeclaredLocallyBeforeClass.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
			beforeListener.beforeTestMethod(testContext);
			afterListener.beforeTestMethod(testContext);
			afterListener.afterTestMethod(testContext);
			beforeListener.afterTestMethod(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void declaredLocallyOnClassAfterClass() throws Exception {
			Class<?> clazz = DirtiesContextDeclaredLocallyAfterClass.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
			beforeListener.beforeTestMethod(testContext);
			afterListener.beforeTestMethod(testContext);
			afterListener.afterTestMethod(testContext);
			beforeListener.afterTestMethod(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void declaredViaMetaAnnotationOnClassAfterClass() throws Exception {
			Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterClass.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
			beforeListener.beforeTestMethod(testContext);
			afterListener.beforeTestMethod(testContext);
			afterListener.afterTestMethod(testContext);
			beforeListener.afterTestMethod(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void beforeAndAfterTestMethodForDirtiesContextViaMetaAnnotationWithOverrides() throws Exception {
			Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverrides.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
			beforeListener.beforeTestMethod(testContext);
			afterListener.beforeTestMethod(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
			afterListener.afterTestMethod(testContext);
			beforeListener.afterTestMethod(testContext);
			verify(testContext, times(1)).markApplicationContextDirty(CURRENT_LEVEL);
		}
	}

	@Nested
	@DisplayName("Before and after test class")
	class BeforeAndAfterTestClassTests {

		@Test
		void declaredLocallyOnMethod() throws Exception {
			Class<?> clazz = getClass().getEnclosingClass();
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			beforeListener.beforeTestClass(testContext);
			afterListener.beforeTestClass(testContext);
			afterListener.afterTestClass(testContext);
			beforeListener.afterTestClass(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void declaredLocallyOnClassBeforeEachTestMethod() throws Exception {
			Class<?> clazz = DirtiesContextDeclaredLocallyBeforeEachTestMethod.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			beforeListener.beforeTestClass(testContext);
			afterListener.beforeTestClass(testContext);
			afterListener.afterTestClass(testContext);
			beforeListener.afterTestClass(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void declaredLocallyOnClassAfterEachTestMethod() throws Exception {
			Class<?> clazz = DirtiesContextDeclaredLocallyAfterEachTestMethod.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			beforeListener.beforeTestClass(testContext);
			afterListener.beforeTestClass(testContext);
			afterListener.afterTestClass(testContext);
			beforeListener.afterTestClass(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void declaredViaMetaAnnotationOnClassAfterEachTestMethod() throws Exception {
			Class<?> clazz = DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			beforeListener.beforeTestClass(testContext);
			afterListener.beforeTestClass(testContext);
			afterListener.afterTestClass(testContext);
			beforeListener.afterTestClass(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void declaredLocallyOnClassBeforeClass() throws Exception {
			assertBeforeClass(DirtiesContextDeclaredLocallyBeforeClass.class);
		}

		@Test
		void declaredLocallyOnClassAfterClass() throws Exception {
			assertAfterClass(DirtiesContextDeclaredLocallyAfterClass.class);
		}

		@Test
		void declaredViaMetaAnnotationOnClassAfterClass() throws Exception {
			assertAfterClass(DirtiesContextDeclaredViaMetaAnnotationAfterClass.class);
		}

		@Test
		void declaredViaMetaAnnotationWithOverrides() throws Exception {
			Class<?> clazz = DirtiesContextViaMetaAnnotationWithOverrides.class;
			BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
			beforeListener.beforeTestClass(testContext);
			afterListener.beforeTestClass(testContext);
			afterListener.afterTestClass(testContext);
			beforeListener.afterTestClass(testContext);
			verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		}

		@Test
		void declaredViaMetaAnnotationWithOverriddenAttributes() throws Exception {
			assertAfterClass(DirtiesContextViaMetaAnnotationWithOverriddenAttributes.class);
		}
	}

	@Nested
	@DisplayName("Nested config - before and after test method modes")
	class NestedBeforeAndAfterTestMethodTests {

		@Test
		void onTopLevelClassWithBeforeEachTestMethod() throws Exception {
			assertBeforeMethod(BeforeAndAfterTestMethodTopLevelClass.class);
		}

		@Test
		void onNestedClassWithConfigOverriddenByDefaultWithAfterClass() throws Exception {
			assertAfterMethod(BeforeAndAfterTestMethodTopLevelClass.ConfigOverriddenByDefault.class);
		}

		@Test
		void onNestedClassWithInheritedConfigWithBeforeEachTestMethod() throws Exception {
			assertBeforeMethod(BeforeAndAfterTestMethodTopLevelClass.InheritedConfig.class);
		}

		@Test
		void onNestedClassWithOverriddenConfigWithAfterClass() throws Exception {
			assertAfterMethod(BeforeAndAfterTestMethodTopLevelClass.OverriddenConfig.class);
		}

		@Test
		void onNestedClassWithInheritedConfigButOverriddenWithBeforeEachTestMethod() throws Exception {
			assertBeforeMethod(BeforeAndAfterTestMethodTopLevelClass.OverriddenConfig.InheritedConfigButOverridden.class);
		}
	}

	@Nested
	@DisplayName("Nested config - before and after test class modes")
	class NestedBeforeAndAfterTestClassTests {

		@Test
		void onTopLevelClassWithBeforeClass() throws Exception {
			assertBeforeClass(BeforeAndAfterTestClassTopLevelClass.class);
		}

		@Test
		void onNestedClassWithConfigOverriddenByDefaultWithAfterClass() throws Exception {
			assertAfterClass(BeforeAndAfterTestClassTopLevelClass.ConfigOverriddenByDefault.class);
		}

		@Test
		void onNestedClassWithInheritedConfigWithBeforeClass() throws Exception {
			assertBeforeClass(BeforeAndAfterTestClassTopLevelClass.InheritedConfig.class);
		}

		@Test
		void onNestedClassWithOverriddenConfigWithAfterClass() throws Exception {
			assertAfterClass(BeforeAndAfterTestClassTopLevelClass.OverriddenConfig.class);
		}

		@Test
		void onNestedClassWithInheritedConfigButOverriddenWithBeforeClass() throws Exception {
			assertBeforeClass(BeforeAndAfterTestClassTopLevelClass.OverriddenConfig.InheritedConfigButOverridden.class);
		}
	}


	private void assertBeforeMethod(Class<?> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	private void assertAfterMethod(Class<?> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		given(testContext.getTestMethod()).willReturn(clazz.getDeclaredMethod("test"));
		beforeListener.beforeTestMethod(testContext);
		afterListener.beforeTestMethod(testContext);
		verify(testContext, times(0)).markApplicationContextDirty(any(HierarchyMode.class));
		afterListener.afterTestMethod(testContext);
		beforeListener.afterTestMethod(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	private void assertBeforeClass(Class<?> clazz) throws Exception {
		BDDMockito.<Class<?>> given(testContext.getTestClass()).willReturn(clazz);
		beforeListener.beforeTestClass(testContext);
		afterListener.beforeTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
		afterListener.afterTestClass(testContext);
		beforeListener.afterTestClass(testContext);
		verify(testContext, times(1)).markApplicationContextDirty(EXHAUSTIVE);
	}

	private void assertAfterClass(Class<?> clazz) throws Exception {
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
	}

	@DirtiesContext
	void dirtiesContextDeclaredLocallyWithAfterMethodMode() {
	}

	@MetaDirtyAfterMethod
	void dirtiesContextDeclaredViaMetaAnnotationWithAfterMethodMode() {
	}


	@DirtiesContext
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaDirtyAfterMethod {
	}

	@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaDirtyAfterEachTestMethod {
	}

	@DirtiesContext(classMode = AFTER_CLASS)
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaDirtyAfterClass {
	}

	@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
	static class DirtiesContextDeclaredLocallyBeforeEachTestMethod {

		void test() {
		}
	}

	@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
	static class DirtiesContextDeclaredLocallyAfterEachTestMethod {

		void test() {
		}
	}

	@DirtiesContext
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaDirtyWithOverrides {

		@AliasFor(annotation = DirtiesContext.class)
		ClassMode classMode() default AFTER_EACH_TEST_METHOD;

		@AliasFor(annotation = DirtiesContext.class)
		HierarchyMode hierarchyMode() default HierarchyMode.CURRENT_LEVEL;
	}

	@MetaDirtyAfterEachTestMethod
	static class DirtiesContextDeclaredViaMetaAnnotationAfterEachTestMethod {

		void test() {
		}
	}

	@DirtiesContext(classMode = BEFORE_CLASS)
	static class DirtiesContextDeclaredLocallyBeforeClass {

		void test() {
		}
	}

	@DirtiesContext(classMode = AFTER_CLASS)
	static class DirtiesContextDeclaredLocallyAfterClass {

		void test() {
		}
	}

	@MetaDirtyAfterClass
	static class DirtiesContextDeclaredViaMetaAnnotationAfterClass {

		void test() {
		}
	}

	@MetaDirtyWithOverrides
	static class DirtiesContextViaMetaAnnotationWithOverrides {

		void test() {
		}
	}

	@MetaDirtyWithOverrides(classMode = AFTER_CLASS, hierarchyMode = EXHAUSTIVE)
	static class DirtiesContextViaMetaAnnotationWithOverriddenAttributes {

		void test() {
		}
	}

	@DirtiesContext(classMode = BEFORE_CLASS)
	static class BeforeAndAfterTestClassTopLevelClass {

		void test() {
		}


		@DirtiesContext(classMode = AFTER_CLASS)
		class ConfigOverriddenByDefault {

			void test() {
			}
		}

		@NestedTestConfiguration(INHERIT)
		class InheritedConfig {

			void test() {
			}
		}

		@NestedTestConfiguration(OVERRIDE)
		@DirtiesContext(classMode = AFTER_CLASS)
		class OverriddenConfig {

			@NestedTestConfiguration(INHERIT)
			@DirtiesContext(classMode = BEFORE_CLASS)
			class InheritedConfigButOverridden {

				void test() {
				}
			}
		}
	}

	@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
	static class BeforeAndAfterTestMethodTopLevelClass {

		void test() {
		}


		@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
		class ConfigOverriddenByDefault {

			void test() {
			}
		}

		@NestedTestConfiguration(INHERIT)
		class InheritedConfig {

			void test() {
			}
		}

		@NestedTestConfiguration(OVERRIDE)
		@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
		class OverriddenConfig {

			void test() {
			}


			@NestedTestConfiguration(INHERIT)
			@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
			class InheritedConfigButOverridden {

				void test() {
				}
			}
		}
	}

}
