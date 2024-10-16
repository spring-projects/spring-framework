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

package org.springframework.test.context.bean.override;

import java.util.List;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * {@code TestExecutionListener} that enables {@link BeanOverride @BeanOverride}
 * support in tests, by injecting overridden beans in appropriate fields of the
 * test instance.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @since 6.2
 */
public class BeanOverrideTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * Executes almost last ({@code LOWEST_PRECEDENCE - 50}).
	 */
	@Override
	public int getOrder() {
		return LOWEST_PRECEDENCE - 50;
	}

	/**
	 * Inject each {@link BeanOverride @BeanOverride} field in the
	 * {@linkplain Object test instance} of the supplied {@linkplain TestContext
	 * test context} with a corresponding bean override instance.
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		injectFields(testContext);
	}

	/**
	 * Re-inject each {@link BeanOverride @BeanOverride} field in the
	 * {@linkplain Object test instance} of the supplied {@linkplain TestContext
	 * test context} with a corresponding bean override instance.
	 * <p>This method does nothing if the
	 * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
	 * REINJECT_DEPENDENCIES_ATTRIBUTE} attribute is not present in the
	 * {@code TestContext} with a value of {@link Boolean#TRUE}.
	 */
	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		Object reinjectDependenciesAttribute = testContext.getAttribute(
				DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE);
		if (Boolean.TRUE.equals(reinjectDependenciesAttribute)) {
			injectFields(testContext);
		}
	}

	/**
	 * Inject each {@link BeanOverride @BeanOverride} field in the test instance with
	 * a corresponding bean override instance.
	 */
	private static void injectFields(TestContext testContext) {
		List<BeanOverrideHandler> handlers = BeanOverrideHandler.forTestClass(testContext.getTestClass());
		if (!handlers.isEmpty()) {
			Object testInstance = testContext.getTestInstance();
			BeanOverrideRegistry beanOverrideRegistry = testContext.getApplicationContext()
					.getBean(BeanOverrideContextCustomizer.REGISTRY_BEAN_NAME, BeanOverrideRegistry.class);

			for (BeanOverrideHandler handler : handlers) {
				beanOverrideRegistry.inject(testInstance, handler);
			}
		}
	}

}
