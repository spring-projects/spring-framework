/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

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
	 * The {@link #getOrder() order} value for this listener: {@value}.
	 * @since 6.2.3
	 */
	public static final int ORDER = 1950;

	/**
	 * Returns {@value #ORDER}, which ensures that the {@code BeanOverrideTestExecutionListener}
	 * is ordered after the
	 * {@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
	 * DirtiesContextBeforeModesTestExecutionListener} and before the
	 * {@link DependencyInjectionTestExecutionListener}.
	 */
	@Override
	public int getOrder() {
		return ORDER;
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
			ApplicationContext applicationContext = testContext.getApplicationContext();

			Assert.state(applicationContext.containsBean(BeanOverrideRegistry.BEAN_NAME), () -> """
					Test class %s declares @BeanOverride fields %s, but no BeanOverrideHandler has been registered. \
					If you are using @ContextHierarchy, ensure that context names for bean overrides match \
					configured @ContextConfiguration names.""".formatted(testContext.getTestClass().getSimpleName(),
							handlers.stream().map(BeanOverrideHandler::getField).filter(Objects::nonNull)
								.map(Field::getName).toList()));
			BeanOverrideRegistry beanOverrideRegistry = applicationContext.getBean(BeanOverrideRegistry.BEAN_NAME,
					BeanOverrideRegistry.class);

			for (BeanOverrideHandler handler : handlers) {
				Field field = handler.getField();
				Assert.state(field != null, () -> "BeanOverrideHandler must have a non-null field: " + handler);
				Object bean = beanOverrideRegistry.getBeanForHandler(handler, field.getType());
				Assert.state(bean != null, () -> """
						No bean override instance found for BeanOverrideHandler %s. If you are using \
						@ContextHierarchy, ensure that context names for bean overrides match configured \
						@ContextConfiguration names.""".formatted(handler));
				injectField(field, testInstance, bean);
			}
		}
	}

	private static void injectField(Field field, Object target, Object bean) {
		try {
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, target, bean);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Could not inject field '" + field + "'", ex);
		}
	}

}
