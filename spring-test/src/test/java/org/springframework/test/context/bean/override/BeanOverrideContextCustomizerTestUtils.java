/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

import static org.mockito.Mockito.mock;

/**
 * Test utilities for {@link BeanOverrideContextCustomizer} that are public so
 * that specific bean override implementations can use them.
 *
 * @author Stephane Nicoll
 */
public abstract class BeanOverrideContextCustomizerTestUtils {

	private static final BeanOverrideContextCustomizerFactory factory = new BeanOverrideContextCustomizerFactory();

	/**
	 * Create a {@link ContextCustomizer} for the given {@code testClass}. Return
	 * a customizer to handle any use of {@link BeanOverride} or {@code null} if
	 * the test class does not use them.
	 * @param testClass a test class to introspect
	 * @return a context customizer for bean override support, or null
	 */
	public static @Nullable ContextCustomizer createContextCustomizer(Class<?> testClass) {
		return factory.createContextCustomizer(testClass, List.of(new ContextConfigurationAttributes(testClass)));
	}

	/**
	 * Customize the given {@linkplain ConfigurableApplicationContext application
	 * context} for the given {@code testClass}.
	 * @param testClass the test to process
	 * @param context the context to customize
	 */
	public static void customizeApplicationContext(Class<?> testClass, ConfigurableApplicationContext context) {
		ContextCustomizer contextCustomizer = createContextCustomizer(testClass);
		if (contextCustomizer != null) {
			contextCustomizer.customizeContext(context, mock(MergedContextConfiguration.class));
		}
	}

}
