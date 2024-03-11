/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * {@code TestExecutionListener} that enables {@link MockitoBean @MockitoBean} and
 * {@link MockitoSpyBean @MockitoSpyBean} support. Also triggers
 * {@link MockitoAnnotations#openMocks(Object)} when any Mockito annotations are
 * used, primarily to support {@link Captor @Captor} annotations.
 *
 * <p>The automatic reset support for {@code @MockBean} and {@code @SpyBean} is
 * handled by the {@link MockitoResetTestExecutionListener}.
 *
 * @author Simon Baslé
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 6.2
 * @see MockitoResetTestExecutionListener
 */
public class MockitoTestExecutionListener extends AbstractTestExecutionListener {

	private static final String MOCKS_ATTRIBUTE_NAME = MockitoTestExecutionListener.class.getName() + ".mocks";

	static final boolean mockitoPresent = ClassUtils.isPresent("org.mockito.MockSettings",
			MockitoTestExecutionListener.class.getClassLoader());


	/**
	 * Executes before {@link DependencyInjectionTestExecutionListener}.
	 */
	@Override
	public final int getOrder() {
		return 1950;
	}

	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		if (mockitoPresent) {
			closeMocks(testContext);
			initMocks(testContext);
		}
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		if (mockitoPresent && Boolean.TRUE.equals(
				testContext.getAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE))) {
			closeMocks(testContext);
			initMocks(testContext);
		}
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (mockitoPresent) {
			closeMocks(testContext);
		}
	}

	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
		if (mockitoPresent) {
			closeMocks(testContext);
		}
	}

	private void initMocks(TestContext testContext) {
		if (hasMockitoAnnotations(testContext)) {
			Object testInstance = testContext.getTestInstance();
			testContext.setAttribute(MOCKS_ATTRIBUTE_NAME, MockitoAnnotations.openMocks(testInstance));
		}
	}

	private void closeMocks(TestContext testContext) throws Exception {
		Object mocks = testContext.getAttribute(MOCKS_ATTRIBUTE_NAME);
		if (mocks instanceof AutoCloseable closeable) {
			closeable.close();
		}
	}

	private boolean hasMockitoAnnotations(TestContext testContext) {
		MockitoAnnotationCollector collector = new MockitoAnnotationCollector();
		ReflectionUtils.doWithFields(testContext.getTestClass(), collector);
		return collector.hasAnnotations();
	}


	/**
	 * {@link FieldCallback} that collects Mockito annotations.
	 */
	private static final class MockitoAnnotationCollector implements FieldCallback {

		private final Set<Annotation> annotations = new LinkedHashSet<>();

		@Override
		public void doWith(Field field) throws IllegalArgumentException {
			for (Annotation annotation : field.getAnnotations()) {
				if (annotation.annotationType().getPackageName().startsWith("org.mockito")) {
					this.annotations.add(annotation);
				}
			}
		}

		boolean hasAnnotations() {
			return !this.annotations.isEmpty();
		}

	}

}
