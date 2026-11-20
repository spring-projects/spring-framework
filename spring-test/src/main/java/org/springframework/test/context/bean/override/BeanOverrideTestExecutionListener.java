/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		Object testInstance = testContext.getTestInstance();
		// Since JUnit Jupiter 5.12, if the SpringExtension is used with Jupiter's
		// TEST_METHOD ExtensionContextScope, the value returned from
		// testContext.getTestClass() may refer to the declaring class of the test
		// method which is about to be invoked (which may be in a @Nested class
		// within the class for the test instance). Thus, we use the class for the
		// test instance as the "test class".
		Class<?> testClass = testInstance.getClass();

		List<BeanOverrideHandler> handlers = BeanOverrideUtils.findHandlersForFields(testClass);
		if (!handlers.isEmpty()) {
			ApplicationContext applicationContext = testContext.getApplicationContext();

			Assert.state(applicationContext.containsBean(BeanOverrideRegistry.BEAN_NAME), () -> """
					Test class %s declares @BeanOverride fields %s, but no BeanOverrideHandler has been registered. \
					If you are using @ContextHierarchy, ensure that context names for bean overrides match \
					configured @ContextConfiguration names.""".formatted(testClass.getSimpleName(),
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
