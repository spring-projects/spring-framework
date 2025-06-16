/*
 * Copyright 2002-2025 the original author or authors.
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
