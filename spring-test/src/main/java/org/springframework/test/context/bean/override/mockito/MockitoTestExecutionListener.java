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

package org.springframework.test.context.bean.override.mockito;

import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.ClassUtils;

/**
 * {@code TestExecutionListener} that enables {@link MockitoBean @MockitoBean}
 * and {@link MockitoSpyBean @MockitoSpyBean} support. Also triggers Mockito
 * setup of a {@link Mockito#mockitoSession() session} for each test class that
 * uses these annotations (or any annotation in that package).
 *
 * <p>The {@link MockitoSession#setStrictness(Strictness) strictness} of the
 * session defaults to {@link Strictness#STRICT_STUBS}. Use
 * {@link MockitoBeanSettings @MockitoBeanSettings} to specify a different strictness.
 *
 * <p>The automatic reset support for {@code @MockBean} and {@code @SpyBean} is
 * handled by the {@link MockitoResetTestExecutionListener}.
 *
 * @author Simon Baslé
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoResetTestExecutionListener
 * @see MockitoBean @MockitoBean
 * @see MockitoSpyBean @MockitoSpyBean
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
		Class<?> testClass = testContext.getTestClass();
		if (MockitoAnnotationDetector.hasMockitoAnnotations(testClass)) {
			Object testInstance = testContext.getTestInstance();
			MockitoBeanSettings annotation = AnnotationUtils.findAnnotation(testClass, MockitoBeanSettings.class);
			Strictness strictness = (annotation != null ? annotation.value() : Strictness.STRICT_STUBS);
			testContext.setAttribute(MOCKS_ATTRIBUTE_NAME, initMockitoSession(testInstance, strictness));
		}
	}

	private MockitoSession initMockitoSession(Object testInstance, Strictness strictness) {
		return Mockito.mockitoSession()
				.initMocks(testInstance)
				.strictness(strictness)
				.startMocking();
	}

	private void closeMocks(TestContext testContext) throws Exception {
		Object mocks = testContext.getAttribute(MOCKS_ATTRIBUTE_NAME);
		if (mocks instanceof MockitoSession session) {
			session.finishMocking();
		}
	}

}
